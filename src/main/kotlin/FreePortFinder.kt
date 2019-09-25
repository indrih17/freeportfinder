import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

/**
 * Finds currently available server ports.
 */
object FreePortFinder {
    private val mutex = Mutex()

    /**
     * The minimum server currentMinPort number for IPv4.
     * Set at 1100 to avoid returning privileged currentMinPort numbers.
     */
    const val minPortNumber = 1100

    /**
     * The maximum server currentMinPort number for IPv4.
     */
    const val maxPortNumber = 65535

    private val initMinPort: Int

    init {
        var port = minPortNumber
        var serverSocket: ServerSocket? = null

        while (serverSocket == null) {
            serverSocket = try {
                ServerSocket(port)
            } catch (e: Exception) {
                port += 200
                null
            }
        }

        initMinPort = port

        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                try {
                    serverSocket.close()
                } catch (ex: Exception) {
                    // ignore
                }
            }
        })
    }

    /**
     * Incremented to the next lowest available port when findFreeLocalPort() is called.
     */
    private val currentMinPort = MutexPrimitive(initMinPort)

    /**
     * Gets the next available port starting at a given from port.
     *
     * @param fromPort the from port to scan for availability
     * @param bindAddress the address that will try to bind
     * @return the available port or failure.
     */
    suspend fun findFreeLocalPort(
        fromPort: Int? = null,
        bindAddress: InetAddress? = null
    ): Either<Failure, Int> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val minPort = fromPort ?: currentMinPort.get()
            if (minPort < currentMinPort.get() || minPort > maxPortNumber)
                Either.left(Failure.IllegalArgument("From port number not in valid range: $minPort"))
            else
                (minPort..maxPortNumber)
                    .firstOrNull { available(it, bindAddress) }
                    ?.let {
                        if (fromPort == null) currentMinPort.set(it + 1)
                        Either.right(it)
                    }
                    ?: Either.left(Failure.NoSuchElement("Could not find an available port above $minPort"))
        }
    }

    /**
     * Gets the next available port starting at a given from port.
     *
     * @param bindAddresses the addresses that will try to bind
     * @return the available port or failure.
     */
    suspend fun findFreeLocalPortOnAddresses(bindAddresses: List<InetAddress?>): Either<Failure, Int> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val fromPort = currentMinPort.get()
            if (fromPort > maxPortNumber)
                Either.left(Failure.IllegalArgument("From port number not in valid range: $fromPort"))
            else
                (fromPort..maxPortNumber)
                    .firstOrNull { containsAvailable(it, bindAddresses) }
                    ?.let {
                        currentMinPort.set(it + 1)
                        Either.right(it)
                    }
                    ?: Either.left(Failure.NoSuchElement("Could not find an available port above $fromPort"))
        }
    }

    private fun containsAvailable(port: Int, bindAddressList: List<InetAddress?>): Boolean =
        bindAddressList.firstOrNull { available(port, it) } != null

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port number to check for availability
     * @param bindAddress the address that will try to bind
     * @return <tt>true</tt> if the port is available, or <tt>false</tt> if not
     */
    private fun available(port: Int, bindAddress: InetAddress? = null): Boolean =
        try {
            val serverSocket = if (bindAddress != null)
                ServerSocket(port, 50, bindAddress)
            else
                ServerSocket(port)

            serverSocket.use {
                serverSocket.reuseAddress = true
                val datagramSocket = bindAddress?.let { DatagramSocket(port, it) } ?: DatagramSocket(port)
                datagramSocket.use {
                    datagramSocket.reuseAddress = true
                }
            }
            true
        } catch (e: IOException) {
            false
        }

    sealed class Failure(val message: String) {
        class IllegalArgument(message: String) : Failure(message)
        class NoSuchElement(message: String) : Failure(message)
    }
}