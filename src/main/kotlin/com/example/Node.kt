package com.example

import com.example.data.model.Block
import com.example.data.model.Message
import com.example.data.model.MessageType
import com.example.utils.receiveMessage
import com.example.utils.sendMessage
import kotlinx.coroutines.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.internal.synchronized
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class Node(private val inAddress: SocketAddress) {

    private val nodeId = UUID.randomUUID()
    private val otherNodes = mutableListOf<SocketAddress>()
    private val currentBlock = AtomicReference<Block>()
    private val blocks = mutableSetOf<Block>()
    private val mutex = Mutex()

    suspend fun start(isStarted: Boolean) = coroutineScope {
        if (isStarted) {
            val firstBlock = Block.createBlock(null)
            processBlock(firstBlock, updateCurrent = true, isGenerated = true)
        }

        generateBlocks()
        processInboundMessages()
    }

    fun notifyAboutOtherNodes(nodes: List<SocketAddress>) {
        otherNodes.addAll(nodes - this.inAddress)
    }

    private suspend fun processBlock(block: Block, updateCurrent: Boolean, isGenerated: Boolean) {

        if (updateCurrent) currentBlock.set(block)

        updateBlock(block)

        mutex.withLock {
            if (!blocks.contains(block)) blocks.add(block)
        }
    }

    private fun CoroutineScope.generateBlocks() = launch(Dispatchers.IO) {
        while (isActive) {
            val oldBlock = currentBlock.get()
            if (oldBlock != null) {
                val block = Block.createBlock(oldBlock.index, oldBlock.hash)
                val newBlock = getNewBlock()

                if (oldBlock != newBlock) {
                    processBlock(newBlock, updateCurrent = true, isGenerated = false)
                } else {
                    processBlock(block, updateCurrent = false, isGenerated = true)
                }
            }
        }
    }

    private suspend fun processInboundMessages() = coroutineScope {
        launch(Dispatchers.IO) {
            val serverSocket = aSocket(SelectorManager(Dispatchers.IO))
                .tcp().bind(inAddress)

            while (isActive) {
                val socket = serverSocket.accept()

                val receivedMessage = socket.openReadChannel().receiveMessage()

                when (receivedMessage.type) {
                    MessageType.UPD_BLOCK -> {
                        val oldBlock = currentBlock.get()
                        val receivedBlock = receivedMessage.block!!
                        if (oldBlock != null) {
                            if (receivedBlock.index - oldBlock.index == 1 &&
                                oldBlock.hash == receivedBlock.prevHash
                            ) {
                                val isSet = currentBlock.compareAndSet(oldBlock, receivedBlock)
                                processBlock(
                                    if (isSet) receivedBlock else getNewBlock(),
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

                    else -> {}
                }
            }
        }
    }

    private suspend fun updateBlock(block: Block) {
        val selectorManager = SelectorManager(Dispatchers.IO)
        val message = Message(MessageType.UPD_BLOCK, nodeId.toString(), block)

        otherNodes.forEach {
            aSocket(selectorManager)
                .tcp()
                .connect(it)
                .openWriteChannel(autoFlush = true)
                .sendMessage(message)
        }
    }

    private suspend fun getNewBlock(): Block {
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

