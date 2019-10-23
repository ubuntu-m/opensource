package io.jopen.memdb.base.storage;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import io.jopen.core.common.reflect.ReflectHelper;
import io.jopen.core.function.ReturnValue;
import io.jopen.memdb.base.annotation.Entity;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author maxuefeng
 * @since 2019/10/22
 * <p>{@link com.google.common.collect.Table}</p>
 */
final
class JavaModelTable<T> implements Serializable {

    // 数据存储
    private CopyOnWriteArrayList<T> cells = new CopyOnWriteArrayList<>();

    // primary key
    private Set<Object> ids = new ConcurrentSkipListSet<>();

    // 修改表格之前的回调函数
    static List<PreModifyTableAction> preModifyTableActions = new ArrayList<>();

    static {
        // 修改表格之前预操作
        preModifyTableActions.add((database, object) -> {
            JavaModelTable javaModelTable = database.getTable(parseEntity(object.getClass()));

            if (javaModelTable == null) {
                javaModelTable = new JavaModelTable(object.getClass());
                database.tables.put(javaModelTable.getTableName(), javaModelTable);
            }
            return ReturnValue.of(object.getClass().getName(), javaModelTable);
        });
    }

    // 存储的目标类型
    private Class<T> target;

    private String tableName;

    JavaModelTable(Class<T> target) {
        Preconditions.checkNotNull(target);
        this.target = target;
        this.tableName = parseEntity(target);
    }

    public String getTableName() {
        return this.tableName;
    }

    T queryOne(T t) {
        Optional<T> optional = cells.parallelStream().filter(cell -> {
            // 获取指定属性匹配值
            // key fieldName value fieldValue
            Map<String, Object> filedNameValues = ReflectHelper.getObjFiledValues(t);

            Set<Field> fieldSet = Arrays.stream(cell.getClass().getFields())
                    .filter(field -> filedNameValues.containsKey(field.getName()))
                    .collect(Collectors.toSet());

            // 进行断言
            List<Predicate<T>> predicateList = new ArrayList<>();

            // 循环添加不为空字段必须相同
            fieldSet.forEach(field -> predicateList.add(input -> {
                try {
                    // 当前cell的值要匹配才可以
                    Object objFiledValue = ReflectHelper.getObjFiledValue(cell, field.getName());
                    assert objFiledValue != null;
                    return objFiledValue.equals(filedNameValues.get(field.getName()));
                } catch (NoSuchFieldException ignored) {
                    return false;
                }
            }));

            // 断言结果
            Predicate<T> predicate = Predicates.and(predicateList);

            // 进行过滤
            return predicate.apply(cell);
        }).findFirst();

        return optional.orElse(null);
    }

    List<T> queryList() {
        return cells.parallelStream().filter(t -> false).collect(Collectors.toList());
    }

    Boolean add(T t) {
        return cells.addIfAbsent(t);
    }

    public void delete(T t) throws Throwable {
        Optional<T> optional = cells.parallelStream().filter(cell -> cell.equals(t)).findFirst();
        T target = optional.orElseThrow((Supplier<Throwable>) RuntimeException::new);
        // this.cells.removeIf()
        this.cells.remove(target);


    }

    @Override
    public String toString() {

        StringBuilder column = new StringBuilder();
        List<String> fieldNames = Arrays.stream(this.target.getFields()).map(Field::getName).collect(Collectors.toList());
        fieldNames.forEach(fieldName -> column.append(" ").append(fieldName).append("                   "));

        // 拼接换行
        column.append("\n");
        column.append("\n");

        StringBuilder value = new StringBuilder();
        for (T cell : this.cells) {
            fieldNames.forEach(fieldName -> {
                try {
                    Field field = cell.getClass().getField(fieldName);
                    field.setAccessible(true);
                    Object obj = field.get(cell);
                    if (obj == null) {
                        value.append(" ");
                    } else {
                        value.append(" ").append(obj.toString());
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            value.append("\n");
            value.append("\n");
        }
        return column.append(value).toString();
    }


    static <T> String parseEntity(Class<T> clazz) {
        Entity annotation = clazz.getAnnotation(Entity.class);
        if (annotation == null || Strings.isNullOrEmpty(annotation.value())) {
            return clazz.getSimpleName();
        }
        return annotation.value();
    }
}
