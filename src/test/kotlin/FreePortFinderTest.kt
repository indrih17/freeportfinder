import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.ServerSocket

class FreePortFinderTest {
    @Test
    fun canBindToFoundPort() = runBlocking {
        FreePortFinder
            .findFreeLocalPort()
            .fold(
                ifLeft = ::handleFailure,
                ifRight = { port ->
                    val serverSocket = ServerSocket(port)
                    assertThat(serverSocket.isBound, `is`(true))
                }
            )
    }

    @Test
    fun overTheLimitPortAllocationShouldFail() = runBlocking {
        FreePortFinder
            .findFreeLocalPort(FreePortFinder.maxPortNumber + 1)
            .fold(
                ifLeft = {
                    // all done
                },
                ifRight = {
                    throw IllegalStateException(it.toString())
                }
            )
    }

    @Test
    fun canBindToLocalHostToFoundPort() = runBlocking {
        FreePortFinder
            .findFreeLocalPort(bindAddress = InetAddress.getLocalHost())
            .fold(
                ifLeft = ::handleFailure,
                ifRight = { port ->
                    val serverSocket = ServerSocket(port, 50, InetAddress.getLocalHost())
                    assertThat(serverSocket.isBound, `is`(true))
                }
            )
    }

    @Test
    fun canBindToMultipleHostsToFoundPort() = runBlocking {
        FreePortFinder
            .findFreeLocalPortOnAddresses(listOf(InetAddress.getLocalHost()))
            .fold(
                ifLeft = ::handleFailure,
                ifRight = { port ->
                    val serverSocket = ServerSocket(port, 50, InetAddress.getLocalHost())
                    assertThat(serverSocket.isBound, `is`(true))
                }
            )
    }

    @Test
    fun canBindToMultipleHostsMultipleAllocations() = runBlocking {
        FreePortFinder
            .findFreeLocalPortOnAddresses(listOf(InetAddress.getLocalHost()))
            .fold(
                ifLeft = ::handleFailure,
                ifRight = { port ->
                    FreePortFinder
                        .findFreeLocalPortOnAddresses(listOf(null, InetAddress.getLocalHost()))
                        .fold(
                            ifLeft = ::handleFailure,
                            ifRight = { nextPort ->
                                assertThat(nextPort > port, `is`(true))
                            }
                        )
                }
            )
    }

    private fun handleFailure(fail: FreePortFinder.Failure): Unit =
        when (fail) {
            is FreePortFinder.Failure.IllegalArgument ->
                throw IllegalArgumentException(fail.message)

            is FreePortFinder.Failure.NoSuchElement ->
                throw NoSuchElementException(fail.message)
        }
}
