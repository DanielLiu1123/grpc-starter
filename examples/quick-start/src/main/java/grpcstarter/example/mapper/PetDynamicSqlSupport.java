package grpcstarter.example.mapper;

import grpcstarter.example.entity.Status;
import jakarta.annotation.Generated;
import java.sql.JDBCType;
import org.mybatis.dynamic.sql.AliasableSqlTable;
import org.mybatis.dynamic.sql.SqlColumn;

public final class PetDynamicSqlSupport {
    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    public static final Pet pet = new Pet();

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source field: pet.id")
    public static final SqlColumn<Integer> id = pet.id;

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source field: pet.name")
    public static final SqlColumn<String> name = pet.name;

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source field: pet.status")
    public static final SqlColumn<Status> status = pet.status;

    @Generated(value = "org.mybatis.generator.api.MyBatisGenerator", comments = "Source Table: pet")
    public static final class Pet extends AliasableSqlTable<Pet> {
        public final SqlColumn<Integer> id = column("id", JDBCType.INTEGER);

        public final SqlColumn<String> name = column("name", JDBCType.VARCHAR);

        public final SqlColumn<Status> status = column(
                        "status", JDBCType.INTEGER, "org.apache.ibatis.type.EnumOrdinalTypeHandler")
                .withJavaType(Status.class);

        public Pet() {
            super("pet", Pet::new);
        }
    }
}
