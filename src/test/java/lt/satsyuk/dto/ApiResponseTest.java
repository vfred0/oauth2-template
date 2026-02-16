package lt.satsyuk.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void toStringIncludesFields() {
        ApiResponse<String> response = ApiResponse.ok("payload");

        assertThat(response.toString())
                .contains("ApiResponse")
                .contains("code=0")
                .contains("data=payload")
                .contains("message=OK");
    }

    @Test
    void toStringIncludesNullDataOnError() {
        ApiResponse<Void> response = ApiResponse.error(ApiResponse.ErrorCode.BAD_REQUEST.getCode(), "Bad request");

        assertThat(response.toString())
                .contains("ApiResponse")
                .contains("code=40001")
                .contains("data=null")
                .contains("message=Bad request");
    }
}

