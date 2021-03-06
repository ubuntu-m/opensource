package io.jopen.springboot.plugin.mongo.repository;

import com.google.common.collect.Lists;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.query.MongoEntityInformation;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author maxuefeng
 * @see GridFsObject
 * @see org.springframework.data.mongodb.gridfs.GridFsTemplate
 * @see org.springframework.data.mongodb.repository.support.SimpleMongoRepository
 * @since 2020/2/9
 */
@NoRepositoryBean
public class BaseServiceImpl<ID extends Serializable, T, R extends BaseRepository<T, ID>>
        implements BaseService<ID, T> {

    @Autowired
    private R repository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Override
    public <S extends T> Optional<S> findOne(Example<S> example) {
        return this.repository.findOne(example);
    }

    @Override
    public <S extends T> S getOne(Example<S> example) {
        return this.findOne(example).orElse(null);
    }

    @Override
    public <S extends T> Stream<S> stream(Example<S> example) {
        Iterable<S> iterable = this.list(example);
        Collection<S> entities = Lists.newArrayList();
        iterable.iterator().forEachRemaining(entities::add);
        return entities.stream();
    }

    @Override
    public <S extends T> Page<S> page(Example<S> example, Pageable pageable) {
        return this.repository.findAll(example, pageable);
    }

    @Override
    public <S extends T> List<S> listSort(Sort sort) {
        return (List<S>) this.repository.findAll(sort);
    }

    @Override
    public <S extends T> Page<S> page(Pageable pageable) {
        return (Page<S>) this.repository.findAll(pageable);
    }

    @Override
    public <S extends T> List<S> list(Example<S> example) {
        return this.repository.findAll(example);
    }

    @Override
    public <S extends T> List<S> listSort(Example<S> example, Sort sort) {
        return this.repository.findAll(example, sort);
    }

    @Override
    public <S extends T> long count(Example<S> example) {
        return this.repository.count(example);
    }

    @Override
    public <S extends T> boolean exists(Example<S> example) {
        return this.repository.exists(example);
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        return this.repository.saveAll(entities);
    }

    @Override
    public <S extends T> List<S> list() {
        return (List<S>) this.repository.findAll();
    }

    @Override
    public <S extends T> S insert(S entity) {
        return this.repository.insert(entity);
    }

    @Override
    public <S extends T> List<S> insert(Iterable<S> entities) {
        return this.repository.insert(entities);
    }

    @Override
    public <S extends T> S save(S entity) {
        return repository.save(entity);
    }

    @Override
    public <S extends T> Optional<S> findById(ID id) {
        return (Optional<S>) this.repository.findById(id);
    }

    @Override
    public boolean existsById(ID id) {
        return this.repository.existsById(id);
    }

    @Override
    public <S extends T> Iterable<S> findAllById(Iterable<ID> ids) {
        return (Iterable<S>) this.repository.findAllById(ids);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public void deleteById(ID id) {
        this.repository.deleteById(id);
    }

    @Override
    public void delete(T entity) {
        this.repository.delete(entity);
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        this.repository.deleteAll(entities);
    }

    @Override
    public void deleteAll() {
        this.repository.deleteAll();
    }

    @Override
    public <S extends T> Optional<S> findOne(Query query) {
        return (Optional<S>) this.repository.findOne(query);
    }

    @Override
    public <S extends T> S getOne(Query query) {
        return (S) this.repository.getOne(query);
    }

    @Override
    public <S extends T> List<S> list(Query query) {
        return (List<S>) this.repository.list(query);
    }

    @Override
    public <S extends T> Stream<S> stream(Query query) {
        Iterable<T> list = this.list(query);
        Collection<T> objects = new ArrayList<>();
        list.forEach(objects::add);
        return (Stream<S>) objects.stream();
    }

    @Override
    public <S extends T> List<S> listSort(Query query, Sort sort) {
        return (List<S>) this.repository.listSort(query, sort);
    }

    @Override
    public <S extends T> Page<S> page(Query query, Pageable pageable) {
        return this.repository.page(query, pageable);
    }

    @Override
    public <S extends T> long count(Query query) {
        return this.repository.count(query);
    }

    @Override
    public <S extends T> boolean exists(Query query) {
        return this.repository.exists(query);
    }

    @Override
    public List<Map> groupSum(String sumField, String... groupFields) {
        return this.repository.groupSum(sumField, groupFields);
    }

    @Override
    public List<Map> groupSumBy(Criteria criteria, String sumField, String... groupFields) {
        return this.repository.groupSumBy(criteria, sumField, groupFields);
    }

    @Override
    public MapReduceResults<T> mapReduce(String mapFunction, String reduceFunction) {
        return this.repository.mapReduce(mapFunction, reduceFunction);
    }

    @Override
    public <F extends GridFsObject> F findFileBy(Query query) {
        GridFSFile gridFSFile = this.gridFsTemplate.findOne(query);
        return convertGridFsFileToGridFsObject(gridFSFile);
    }

    @Override
    public <F extends GridFsObject> F findFileById(ID id) {
        MongoEntityInformation<T, ID> entityInformation = this.repository.getEntityInformation();
        Query query = new Query();
        // TODO
        GridFSFile gridFSFile = this.gridFsTemplate.findOne(query);
        return convertGridFsFileToGridFsObject(gridFSFile);
    }

    private <F extends GridFsObject> F convertGridFsFileToGridFsObject(GridFSFile gridFSFile) {
        InputStream inputStream = Optional.ofNullable(gridFSFile)
                .map(f -> gridFsTemplate.getResource(gridFSFile))
                .map(s -> {
                    try {
                        return s.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e.getMessage());
                    }
                })
                .orElse(null);

        if (inputStream == null) throw new RuntimeException("inputStream is null");

        String id = String.valueOf(gridFSFile.getId());

//        fileObject.setChunkSize(gridFSFile.getChunkSize());
//        fileObject.setFilename(gridFSFile.getFilename());
//        fileObject.setUploadDate(gridFSFile.getUploadDate());
//        fileObject.setFileStream(inputStream);
//        fileObject.setLength(gridFSFile.getLength());
//        fileObject.setId(id);
//        fileObject.setExtraElements(gridFSFile.getExtraElements());
//        fileObject.setMetadata(gridFSFile.getMetadata());

        F result = (F) this.findById((ID) id).orElse(null);
        try {
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);
            result.setFileBytes(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

    @Override
    public String ensureIndex(Index index) {
        return this.repository.ensureIndex(index);
    }

    @Override
    public List<IndexInfo> getIndexInfo() {
        return this.repository.getIndexInfo();
    }
}
