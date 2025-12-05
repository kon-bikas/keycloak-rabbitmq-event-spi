package hua.spi;


import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;

/*
 * channel lifespan could be shorter than that of its connection. Closing and opening new channels
 * per operation is usually unnecessary but can be appropriate. When in doubt, consider reusing channels first.
 *
 * In general, publishing on a shared "publishing context" (channel in AMQP 0-9-1) should be avoided and considered
 * unsafe. Doing so can result in incorrect framing of data frames on the wire. That leads to connection closure.
 */
public class RabbitMQChannelPool {

    private static final Logger logger = Logger.getLogger(RabbitMQEventListenerProviderFactory.class);

    private static final int DEFAULT_POOL_SIZE = 5;
    /*
     * Create thread-safe queue for threads to be able to borrow a rabbitmq channel
     * to send their event.
     */
    private final BlockingQueue<Channel> workerQueue;

    private final int poolSize;

    private final Connection connection;

    public RabbitMQChannelPool(Connection connection, int poolSize) {
        this.workerQueue = new ArrayBlockingQueue<>(poolSize);
        this.poolSize = poolSize;
        this.connection = connection;
        this.populateBlockingQueue();
    }

    public RabbitMQChannelPool(Connection connection) {
        this(connection, DEFAULT_POOL_SIZE);
    }

    private void populateBlockingQueue() {
        try {
            for (int i = 0; i < this.poolSize; i++) {
                this.addWorker();
            }
        } catch (IOException e) {
            logger.error("Could not create worker queue, can't create channel.", e);
        }
    }

    private void addWorker() throws IOException {
        try {
            Channel channel = this.connection.createChannel();
            this.workerQueue.add(channel);
        } catch (IllegalStateException e) {
            logger.error("trying to add worker in a full queue.", e);
        }
    }

    public Channel lendChannel() {
        try {
            return workerQueue.take();
        } catch (InterruptedException e) {
            logger.error("thread interrupted while waiting for channel.");
            throw new RuntimeException(e);
        }
    }

    public boolean releaseChannel(Channel channel) {
        return workerQueue.offer(channel);
    }

    public void clearWorkerQueue() throws IOException, TimeoutException {
        for (Channel channel : this.workerQueue) {
            channel.close();
        }
        this.workerQueue.clear();
    }

}
