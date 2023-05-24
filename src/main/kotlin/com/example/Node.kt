package com.example

import com.example.data.model.Block
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.utils.receiveMessage
import com.example.utils.sendMessage
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

class Node(
    val inboundAddress: SocketAddress
) {
    private val nodeId = UUID.randomUUID()
    internal val otherNodes = mutableListOf<SocketAddress>()
    private val currentBlock = AtomicReference<Block>()
    internal val blocks = mutableListOf<Block>()
    private val mutex = Mutex()

    suspend fun start(startGeneration: Boolean) = coroutineScope {
        if (startGeneration) {
            val firstBlock = Block.new(null)
            processBlock(firstBlock, updateCurrent = true, isGenerated = true)
        }

        generateBlocks()
        processInboundMessages()
    }

    fun notifyAboutOtherNodes(nodes: Collection<SocketAddress>) {
        otherNodes.addAll(nodes - this.inboundAddress)
    }

    private fun CoroutineScope.generateBlocks() = launch(Dispatchers.IO) {
        while (isActive) {
            val frozenCurrentBlock = currentBlock.get()
            if (frozenCurrentBlock != null) {
                val producedBlock = Block.new(frozenCurrentBlock)
                val theNewestBlock = findTheNewestBlock()

                if (frozenCurrentBlock != theNewestBlock ||
                    !currentBlock.compareAndSet(frozenCurrentBlock, producedBlock)
                ) {
                    processBlock(theNewestBlock, updateCurrent = true, isGenerated = false)
                } else {
                    processBlock(producedBlock, updateCurrent = false, isGenerated = true)
                }
            }
        }
    }

    private fun CoroutineScope.processInboundMessages() {
        launch(Dispatchers.IO) {
            val serverSocket = aSocket(SelectorManager(Dispatchers.IO))
                .tcp().bind(inboundAddress)

            while (isActive) {
                val socket = serverSocket.accept()

                val receivedMessage = socket.openReadChannel().receiveMessage()

                when (receivedMessage.messageType) {
                    MessageType.UPDATE_BLOCK -> {
                        val frozenCurrentBlock = currentBlock.get()
                        val receivedBlock = receivedMessage.block!!
                        if (frozenCurrentBlock != null) {
                            if (receivedBlock.index - frozenCurrentBlock.index == 1 &&
                                frozenCurrentBlock.hash == receivedBlock.previousHash
                            ) {
                                val isSet = currentBlock.compareAndSet(frozenCurrentBlock, receivedBlock)
                                processBlock(
                                    if (isSet) receivedBlock else findTheNewestBlock(),
                                    updateCurrent = !isSet,
                                    isGenerated = false
                                )
                            }
                        } else {
                            processBlock(receivedBlock, updateCurrent = true, isGenerated = false)
                        }
                    }

                    MessageType.REQUEST_BLOCK -> {
                        val currentBlock = currentBlock.get()
                        if (currentBlock != null) {
                            socket.openWriteChannel(autoFlush = true)
                                .sendMessage(Message(MessageType.RESPONSE_BLOCK, nodeId.toString(), currentBlock))
                        }
                    }

                    MessageType.RESPONSE_BLOCK -> {}
                }
            }
        }
    }

    private suspend fun processBlock(block: Block, updateCurrent: Boolean, isGenerated: Boolean) {

        if (updateCurrent) currentBlock.set(block)

        updateBlockAtOtherNodes(block)

        mutex.withLock {
            if (!blocks.contains(block)) blocks.add(block)
        }
    }

    private suspend fun updateBlockAtOtherNodes(producedBlock: Block) {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val message = Message(MessageType.UPDATE_BLOCK, nodeId.toString(), producedBlock)

        otherNodes.forEach {
            aSocket(selectorManager)
                .tcp()
                .connect(it)
                .openWriteChannel(autoFlush = true)
                .sendMessage(message)
        }
    }

    private suspend fun findTheNewestBlock(): Block {
        val frozenCurrentBlock = currentBlock
        otherNodes.forEach {
            val block = requestBlockFrom(it)
            if (block != null &&
                block.index > frozenCurrentBlock.get().index
            ) {
                frozenCurrentBlock.set(block)
            }
        }
        return frozenCurrentBlock.get()
    }

    private suspend fun requestBlockFrom(node: SocketAddress): Block? {
        val message = Message(MessageType.REQUEST_BLOCK, nodeId.toString())

        val socket = aSocket(SelectorManager(Dispatchers.IO))
            .tcp()
            .connect(node)

        socket.openWriteChannel(autoFlush = true)
            .sendMessage(message)

        return socket.openReadChannel()
            .receiveMessage().block
    }
}