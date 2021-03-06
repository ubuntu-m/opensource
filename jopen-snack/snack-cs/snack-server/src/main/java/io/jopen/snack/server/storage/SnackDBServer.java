package io.jopen.snack.server.storage;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.*;
import io.jopen.snack.common.storage.DBManagement;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;

/**
 * @author maxuefeng
 * @see Service
 * @see AbstractService
 * {@link DBManagement#DBA 初始化DBA信息 }
 * @since 2019/10/23
 */
@Deprecated
public final
class SnackDBServer extends AbstractService {

    private final ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(50));

    // 任务队列
    private final BlockingQueue<Task<Object>> taskBlockingQueue = Queues.newLinkedBlockingQueue();

    private SnackDBServer() {
    }

    // 初始化
    public static final SnackDBServer DB_DATABASE_SYSTEM = new SnackDBServer();

    /**
     * 同步启动方式
     */
    @Override
    protected void doStart() {

        // 加载数据
        // load();

        // 接受任务
    }

    @Override
    protected void doStop() {
        // 将临时文件进行落盘
    }

    void submit(Task<Object> task) {
        this.taskBlockingQueue.add(task);
    }

    void receiveTask() {
        // 开启一个子线程进行执行任务
        new Thread(() -> {

            while (true) {
                try {
                    Task<Object> task = taskBlockingQueue.take();
                    ListenableFuture<Object> future = SnackDBServer.this.service.submit(task);
                    // 添加任务完成回调函数
                    Futures.addCallback(future, task.completeCallback(), service);
                } catch (InterruptedException ignored) {
                }
            }

        }).start();
    }

//    private void load() {
//        File topDir = new File("./memdb");
//        if (topDir.exists()) {
//            File[] files = topDir.listFiles();
//            if (files != null) {
//                Stream.of(files).filter(File::isDirectory).forEach(f -> {
//                    // 获取数据库名称
//                    String dbName = f.getName();
//                    //
//                    String dbPath = "./memdb/" + dbName;
//
//                    File dbFile = new File(dbPath);
//
//                    Database db = new Database(dbName);
//                    File[] tableFiles = dbFile.listFiles();
//
//                    if (tableFiles != null) {
//                        Stream.of(tableFiles).filter(File::isFile).forEach(tf -> {
//                            String className = new String(Base64.getDecoder().decode(tf.getName()));
//                            try {
//                                Class targetClass = Class.forName(className);
//                                // 反序列化
//                                JavaModelTable table = (JavaModelTable) KryoHelper.deserialization(FileHelper.readAllLines(tf.getAbsolutePath()), targetClass);
//                                // 加入对应数据库
//                                db.tables.put(table.getTableName(), table);
//                                // 异常忽略
//                            } catch (ClassNotFoundException | ClassCastException ignored) {
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        });
//                    }
//                    DBManagement.DBA.addDatabase(db);
//                });
//            }
//        }
//    }
}
