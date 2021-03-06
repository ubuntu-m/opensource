package io.jopen.snack.server.operator;

import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import io.jopen.snack.common.DatabaseInfo;
import io.jopen.snack.common.IntermediateExpression;
import io.jopen.snack.common.Row;
import io.jopen.snack.common.TableInfo;
import io.jopen.snack.common.protol.RpcData;
import io.jopen.snack.common.serialize.KryoHelper;
import io.jopen.snack.common.storage.DBManagement;
import io.jopen.snack.common.storage.Database;
import io.jopen.snack.common.storage.RowStoreTable;
import io.jopen.snack.server.tcp.SnackDBTcpServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 解析器顶级父类
 *
 * @author maxuefeng
 * @since 2019/10/27
 */
abstract class Operator {

    protected final DBManagement dbManagement = DBManagement.DBA;


    /**
     * @param requestInfo request info
     * @return response info
     * @see SnackDBTcpServer
     */
    public abstract RpcData.S2C parse(RpcData.C2S requestInfo) throws Exception;

    /**
     * 转换数据
     *
     * @param any
     * @return
     */
    IntermediateExpression<Row> convertByteArray2Expression(Any any) {

        if (any == null) {
            return IntermediateExpression.buildFor(Row.class);
        }

        byte[] bytes = any.getValue().toByteArray();
        try {
            return KryoHelper.deserialization(bytes, IntermediateExpression.class);
        } catch (Exception ignored) {
            return IntermediateExpression.buildFor(Row.class);
        }
    }

    List<IntermediateExpression<Row>> convertByteArray2Expressions(List<Any> anyList) {

        if (anyList == null || anyList.size() == 0) {
            return Lists.newArrayList(IntermediateExpression.buildFor(Row.class));
        }

        List<IntermediateExpression<Row>> expressions = new ArrayList<>();

        for (Any any : anyList) {
            byte[] bytes = any.getValue().toByteArray();
            try {
                IntermediateExpression<Row> expression = KryoHelper.deserialization(bytes, IntermediateExpression.class);
                expressions.add(expression);
            } catch (Exception ignored) {
            }
        }
        return expressions;
    }

    RowStoreTable getTargetTable(RpcData.C2S requestInfo) throws IOException {
        byte[] dbBytes = requestInfo.getDbInfo().toByteArray();
        Database database = dbManagement.securityGetDatabase(KryoHelper.deserialization(dbBytes, DatabaseInfo.class));
        return database.securityGetTable(KryoHelper.deserialization(dbBytes, TableInfo.class));
    }
}
