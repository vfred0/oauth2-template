package lt.satsyuk.service.core.operations.route;

import lt.satsyuk.api.dtos.core.ApiResult;
import lt.satsyuk.service.core.operations.Result;

import java.util.concurrent.CompletableFuture;

public sealed interface RouteResult permits RouteResult.Bulk, RouteResult.Single {

    record Bulk(CompletableFuture<? extends Result<?, ApiResult<Void>>> future)
            implements RouteResult {}

    record Single(Result<?, ApiResult<Void>> result)
            implements RouteResult {}
}
