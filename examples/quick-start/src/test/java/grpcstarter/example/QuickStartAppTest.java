package grpcstarter.example;

import static org.assertj.core.api.Assertions.assertThatCode;

import grpcstarter.example.entity.Pet;
import grpcstarter.example.entity.Status;
import grpcstarter.example.mapper.PetMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class QuickStartAppTest {

    @Autowired
    PetMapper mapper;

    @Test
    void testInsert_whenUsingEnumOrdinalTypeHandler_thenOK() {
        var pet = new Pet();
        pet.setName("test");
        pet.setStatus(Status.NORMAL);

        assertThatCode(() -> mapper.insertSelective(pet)).doesNotThrowAnyException();
    }
}
