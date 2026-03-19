package lt.satsyuk.job;

import lt.satsyuk.service.RequestProcessingService;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.UUID;

public class RequestProcessingJob extends QuartzJobBean {

    public static final String REQUEST_ID_KEY = "requestId";

    @Autowired
    private RequestProcessingService requestProcessingService;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String requestId = jobDataMap.getString(REQUEST_ID_KEY);
        if (requestId == null || requestId.isBlank()) {
            throw new JobExecutionException("Missing requestId in Quartz job data");
        }

        requestProcessingService.processClientCreateRequest(UUID.fromString(requestId));
    }
}

