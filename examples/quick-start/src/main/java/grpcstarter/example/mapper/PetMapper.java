package grpcstarter.example.mapper;

import static grpcstarter.example.mapper.PetDynamicSqlSupport.id;
import static grpcstarter.example.mapper.PetDynamicSqlSupport.name;
import static grpcstarter.example.mapper.PetDynamicSqlSupport.pet;
import static grpcstarter.example.mapper.PetDynamicSqlSupport.status;
import static org.mybatis.dynamic.sql.SqlBuilder.isEqualTo;

import grpcstarter.example.entity.Pet;
import jakarta.annotation.Generated;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectKey;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.type.EnumOrdinalTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.dynamic.sql.BasicColumn;
import org.mybatis.dynamic.sql.delete.DeleteDSLCompleter;
import org.mybatis.dynamic.sql.insert.render.InsertStatementProvider;
import org.mybatis.dynamic.sql.select.CountDSLCompleter;
import org.mybatis.dynamic.sql.select.SelectDSLCompleter;
import org.mybatis.dynamic.sql.select.render.SelectStatementProvider;
import org.mybatis.dynamic.sql.update.UpdateDSL;
import org.mybatis.dynamic.sql.update.UpdateDSLCompleter;
import org.mybatis.dynamic.sql.update.UpdateModel;
import org.mybatis.dynamic.sql.util.SqlProviderAdapter;
import org.mybatis.dynamic.sql.util.mybatis3.CommonCountMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonDeleteMapper;
import org.mybatis.dynamic.sql.util.mybatis3.CommonUpdateMapper;
import org.mybatis.dynamic.sql.util.mybatis3.MyBatis3Utils;

@Mapper
public interface PetMapper extends CommonCountMapper, CommonDeleteMapper, CommonUpdateMapper {
    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    BasicColumn[] selectList = BasicColumn.columnList(id, name, status);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    @InsertProvider(type = SqlProviderAdapter.class, method = "insert")
    @SelectKey(
            statement = "SELECT LAST_INSERT_ID()",
            keyProperty = "row.id",
            before = false,
            resultType = Integer.class)
    int insert(InsertStatementProvider<Pet> insertStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @Results(
            id = "PetResult",
            value = {
                @Result(column = "id", property = "id", jdbcType = JdbcType.INTEGER, id = true),
                @Result(column = "name", property = "name", jdbcType = JdbcType.VARCHAR),
                @Result(
                        column = "status",
                        property = "status",
                        typeHandler = EnumOrdinalTypeHandler.class,
                        jdbcType = JdbcType.INTEGER)
            })
    List<Pet> selectMany(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    @SelectProvider(type = SqlProviderAdapter.class, method = "select")
    @ResultMap("PetResult")
    Optional<Pet> selectOne(SelectStatementProvider selectStatement);

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default long count(CountDSLCompleter completer) {
        return MyBatis3Utils.countFrom(this::count, pet, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int delete(DeleteDSLCompleter completer) {
        return MyBatis3Utils.deleteFrom(this::delete, pet, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int deleteByPrimaryKey(Integer id_) {
        return delete(c -> c.where(id, isEqualTo(id_)));
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int insert(Pet row) {
        return MyBatis3Utils.insert(this::insert, row, pet, c -> c.map(name)
                .toProperty("name")
                .map(status)
                .toProperty("status"));
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int insertSelective(Pet row) {
        return MyBatis3Utils.insert(this::insert, row, pet, c -> c.map(name)
                .toPropertyWhenPresent("name", row::getName)
                .map(status)
                .toPropertyWhenPresent("status", row::getStatus));
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default Optional<Pet> selectOne(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectOne(this::selectOne, selectList, pet, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default List<Pet> select(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectList(this::selectMany, selectList, pet, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default List<Pet> selectDistinct(SelectDSLCompleter completer) {
        return MyBatis3Utils.selectDistinct(this::selectMany, selectList, pet, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default Optional<Pet> selectByPrimaryKey(Integer id_) {
        return selectOne(c -> c.where(id, isEqualTo(id_)));
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int update(UpdateDSLCompleter completer) {
        return MyBatis3Utils.update(this::update, pet, completer);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    static UpdateDSL<UpdateModel> updateAllColumns(Pet row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(name).equalTo(row::getName).set(status).equalTo(row::getStatus);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    static UpdateDSL<UpdateModel> updateSelectiveColumns(Pet row, UpdateDSL<UpdateModel> dsl) {
        return dsl.set(name).equalToWhenPresent(row::getName).set(status).equalToWhenPresent(row::getStatus);
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int updateByPrimaryKey(Pet row) {
        return update(c -> c.set(name)
                .equalTo(row::getName)
                .set(status)
                .equalTo(row::getStatus)
                .where(id, isEqualTo(row::getId)));
    }

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    default int updateByPrimaryKeySelective(Pet row) {
        return update(c -> c.set(name)
                .equalToWhenPresent(row::getName)
                .set(status)
                .equalToWhenPresent(row::getStatus)
                .where(id, isEqualTo(row::getId)));
    }
}
