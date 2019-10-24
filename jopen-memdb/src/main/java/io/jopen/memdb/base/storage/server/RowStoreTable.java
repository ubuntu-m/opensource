package io.jopen.memdb.base.storage.server;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import io.jopen.memdb.base.storage.client.IntermediateExpression;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link JavaModelTable}
 * {@link java.util.concurrent.ConcurrentNavigableMap}
 * {@link java.util.concurrent.ConcurrentMap}
 * {@link java.util.TreeMap}
 *
 * @author maxuefeng
 * @since 2019/10/24
 * TODO   为了方便测试  当前类暂时声明为public类型的
 */
public final
class RowStoreTable implements Serializable {

    // rowsData  TODO   怎么保证数据的有序性
    private final Table<Id, String, Object> rowsData = Tables.newCustomTable(new ConcurrentHashMap<>(), () -> Collections.synchronizedSortedMap(new TreeMap<>()));

    // 全查询最大只能查询1000条数据
    private final int maxAllQueryLimit = 1000;

    // rowsData Name
    private String tableName;

    // 列的属性
    private List<ColumnType> columnTypes;

    // currentDatabase
    private transient Database database;

    RowStoreTable(Database database, String tableName, List<ColumnType> columnTypes) {
        this.database = database;
        this.tableName = tableName;
        this.columnTypes = columnTypes;
    }


    public Table<Id, String, Object> getRowsData() {
        return rowsData;
    }

    public String getTableName() {
        return tableName;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public Database getDatabase() {
        return database;
    }

    // create rowsData Precondition

    // save cell data Precondition  主键为空或者不唯一则返回false
    private Predicate<Row<String, Object>> saveCellPreconditionId = row -> {

        if (row == null) {
            return false;
        }

        // 获取主键
        Id primaryKey = row.getRowKey();

        if (primaryKey == null || primaryKey.size() == 0 || primaryKey.isNull()) {
            return false;
        }

        // 检测主键
        List<ColumnType> primaryKeyColumnTypes = RowStoreTable.this.columnTypes.parallelStream()
                .filter(ColumnType::getPrimaryKey).collect(Collectors.toList());

        // 检测大小
        if (primaryKeyColumnTypes.size() != primaryKey.size()) {
            return false;
        }

        // 检测具体的值是否为空
        Optional<ColumnType> optional = primaryKeyColumnTypes.parallelStream().filter(pct -> row.get(pct.getColumnName()) == null).findAny();
        return optional.isPresent();
    };

    // storage data  consumer the row
    private Consumer<Row<String, Object>> storageRow = row -> {
        Id rowKey = row.getRowKey();
        row.forEach((column, value) -> RowStoreTable.this.rowsData.put(rowKey, column, value));
    };


    /**
     * save data to rowsData
     *
     * @see Row#getRowKey()   storage data to the current rowsData   in row
     * @see com.google.common.collect.Table.Cell#put(Object, Object, Object)
     */
    public void save(Row<String, Object> row) {
        // before save data check data is complete
        // this.rowsData.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        boolean res = saveCellPreconditionId.apply(row);
        if (res) {
            throw new RuntimeException("save exception");
        }

        // put data
        storageRow.accept(row);
    }

    /**
     * save batch  row  data
     *
     * @param rows rows data
     */
    public int saveBatch(@NonNull Collection<Row<String, Object>> rows) {
        int i = 0;
        try {

            rows.forEach(this::save);
            ++i;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return i;
    }


    public List<Id> update(@Nullable IntermediateExpression<Row<String, Object>> expression, HashMap<String, Object> updateBody) {
        List<Row<String, Object>> matchingResult = matching(expression);
        Set<Id> rowKeys = matchingResult.parallelStream().map(Row::getRowKey).collect(Collectors.toSet());

        // TODO  batch update update  注意类型检查   如果类型不匹配 存储没问题  mapper to  java  bean会出现问题
        List<Id> idList = new ArrayList<>();
        this.rowsData.rowMap().forEach((rowKey, columnValues) -> {
            if (rowKeys.contains(rowKey)) {
                columnValues.putAll(updateBody);
                idList.add(rowKey);
            }
        });

        return idList;
    }


    /**
     * 查询
     *
     * @see IntermediateExpression
     */
    @NonNull
    public List<Row<String, Object>> query(@Nullable IntermediateExpression<Row<String, Object>> expression) {
        return matching(expression);
    }


    /**
     * 删除
     *
     * @return delete result
     */
    @NonNull
    public List<Id> delete(@Nullable IntermediateExpression<Row<String, Object>> expression) {
        List<Row<String, Object>> matchingResult = matching(expression);

        if (matchingResult.size() == 0) {
            return Lists.newArrayList();
        }
        List<Id> ids = matchingResult.parallelStream().map(Row::getRowKey).collect(Collectors.toList());


        for (Id id : ids) {
            Map<String, Object> willBeDelRow = this.rowsData.row(id);
            willBeDelRow.forEach((column, value) -> this.rowsData.remove(id, column));
        }

        return ids;
    }

    /**
     * matching condition
     *
     * @param expression
     * @return
     * @see IntermediateExpression#getConditions()
     * @see IntermediateExpression#getTargetClass()
     */
    @NonNull
    private List<Row<String, Object>> matching(@Nullable IntermediateExpression<Row<String, Object>> expression) {
        List<Row<String, Object>> matchingResult;
        // 全查询
        if (expression == null || expression.getConditions().size() == 0) {

            matchingResult = rowsData.rowMap().entrySet().parallelStream().map(entry -> {
                // 主键
                Id rowKey = entry.getKey();
                Row<String, Object> row = new Row<>(rowKey);
                row.putAll(entry.getValue());
                return row;
            }).limit(maxAllQueryLimit).collect(Collectors.toList());
        } else {
            //
            List<IntermediateExpression.Condition<Row<String, Object>>> conditions = expression.getConditions();

            // map the element
            Stream<Row<String, Object>> rowStream = this.rowsData.rowMap().entrySet().parallelStream()

                    // 进行map修改数据
                    .map(entry -> {
                        Id pk = entry.getKey();
                        Row<String, Object> tmpRow = new Row<>(pk);
                        tmpRow.putAll(entry.getValue());
                        return tmpRow;
                    });

            // filter the element
            matchingResult = rowStream.filter(row -> {
                        boolean match = true;
                        for (IntermediateExpression.Condition<Row<String, Object>> condition : conditions) {
                            boolean test = condition.test(row);
                            if (!test) {
                                match = false;
                                break;
                            }
                        }
                        return match;
                    }
            ).collect(Collectors.toList());
        }
        return matchingResult;
    }


    @Override
    public String toString() {

        StringBuilder rowStoreTable = new StringBuilder("RowStoreTable");
        rowStoreTable.append(this.tableName).append("\n");

        // 拼接列名
        String columnNames = Joiner.on("\t\t\t\t\t\t\t\t").join(columnTypes.parallelStream().map(ColumnType::getColumnName).collect(Collectors.toList()));
        rowStoreTable.append(columnNames);
        rowStoreTable.append("\n");

        // 拼接行数据  rowKeySet属于主键集合
        // 获取所有Id
        Set<Id> ids = rowsData.rowKeySet();

        for (Id id : ids) {
            for (ColumnType columnType : this.columnTypes) {
                Object cell = rowsData.get(id, columnType.getColumnName());
                rowStoreTable.append(cell);
                rowStoreTable.append("\t\t\t");
            }
            rowStoreTable.append("\n");
        }
        return rowStoreTable.toString();
    }
}
