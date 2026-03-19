package lt.satsyuk.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppResponseTest {

    @Test
    void toStringIncludesFields() {
        AppResponse<String> response = AppResponse.ok("payload");

        assertThat(response.toString())
                .contains("AppResponse")
                .contains("code=0")
                .contains("data=payload")
                .contains("message=OK");
    }

    @Test
    void toStringIncludesNullDataOnError() {
        AppResponse<Void> response = AppResponse.error(AppResponse.ErrorCode.BAD_REQUEST.getCode(), "Bad request");

        assertThat(response.toString())
                .contains("AppResponse")
                .contains("code=40001")
                .contains("data=null")
                .contains("message=Bad request");
    }
}


