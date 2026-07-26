package at.pegelhub.connector.api;

import at.pegelhub.contact.api.CreateContactDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CreateConnectorDtoTest {
    private static final String LONG_DATA = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void constructor_WhenEverythingWorks() {
        CreateConnectorDto dto = assertDoesNotThrow(() -> new CreateConnectorDto("nr", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes"));

        assertThat(dto.manufacturer()).isNotNull();
        assertThat(dto.softwareManufacturer()).isNotNull();
        assertThat(dto.technicallyResponsible()).isNotNull();
        assertThat(dto.operationCompany()).isNotNull();
    }

    @Test
    void validatorRejectsNullRequiredArgs() {
        CreateConnectorDto dto = new CreateConnectorDto(null, null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes");

        assertThat(invalidFields(dto)).contains("connectorNumber");
    }

    @Test
    void constructorWithNullLegacyFieldsNormalizesToEmptyValues() {
        CreateConnectorDto dto = new CreateConnectorDto("nr", null, null, null,
                null, null, null, null, null, null);

        assertThat(dto.typeDescription()).isEmpty();
        assertThat(dto.softwareVersion()).isEmpty();
        assertThat(dto.worksFromDataVersion()).isEmpty();
        assertThat(dto.dataDefinition()).isEmpty();
        assertThat(dto.notes()).isEmpty();
        assertThat(dto.manufacturer()).isNotNull();
        assertThat(dto.softwareManufacturer()).isNotNull();
        assertThat(dto.technicallyResponsible()).isNotNull();
        assertThat(dto.operationCompany()).isNotNull();
    }

    @Test
    void validatorRejectsEmptyRequiredArgs() {
        CreateConnectorDto dto = new CreateConnectorDto("", null, "desc", "1.0",
                "1.01", "def", null, null, null, "notes");

        assertThat(invalidFields(dto)).contains("connectorNumber");
    }

    @Test
    void validatorRejectsLongArgs() {
        CreateConnectorDto dto = new CreateConnectorDto(LONG_DATA, null, LONG_DATA, LONG_DATA,
                LONG_DATA, LONG_DATA, null, null, null, LONG_DATA);

        assertThat(invalidFields(dto)).contains(
                "connectorNumber",
                "typeDescription",
                "softwareVersion",
                "worksFromDataVersion",
                "dataDefinition",
                "notes");
    }

    @Test
    void validatorRejectsNestedLegacyContactFields() {
        CreateConnectorDto dto = new CreateConnectorDto("nr",
                new CreateContactDto(LONG_DATA, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null),
                "desc", "1.0", "1.01", "def", null, null, null, "notes");

        assertThat(invalidFields(dto)).contains("manufacturer.organization");
    }

    private static Set<String> invalidFields(CreateConnectorDto dto) {
        return VALIDATOR.validate(dto).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
