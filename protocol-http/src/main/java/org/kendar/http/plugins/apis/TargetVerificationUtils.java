package org.kendar.http.plugins.apis;

import org.kendar.annotations.HttpMethodFilter;
import org.kendar.annotations.HttpTypeFilter;
import org.kendar.annotations.TpmDoc;
import org.kendar.annotations.multi.TpmRequest;
import org.kendar.annotations.multi.TpmResponse;
import org.kendar.apis.FilteringClass;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.dtos.TargetVerificationInput;
import org.kendar.di.annotations.TpmService;
import org.kendar.http.plugins.commons.SiteMatcherUtils;
import org.kendar.plugins.apis.Ko;
import org.kendar.plugins.apis.Ok;
import org.kendar.utils.JsonMapper;

import java.util.List;

import static org.kendar.apis.ApiUtils.respondKo;
import static org.kendar.apis.ApiUtils.respondOk;

@TpmService
@HttpTypeFilter()
public class TargetVerificationUtils implements FilteringClass {
    private final JsonMapper mapper;

    public TargetVerificationUtils(JsonMapper mapper) {
        this.mapper = mapper;
    }
    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @HttpMethodFilter(
            pathAddress = "/api/protocols/http/targets/verify",
            method = "POST", id = "POST /api/protocols/http/targets/verify")
    @TpmDoc(
            description = "Verify target data",
            requests = @TpmRequest(
                    body = TargetVerificationInput.class
            ),
            responses = {@TpmResponse(
                    body = Ok.class

            ), @TpmResponse(
                    code = 404,
                    body = Ko.class
            ), @TpmResponse(
                    code = 500,
                    body = Ko.class
            )},
            tags = {"base/storage"})
    public void verifyTarget(Request reqp, Response resp) {
        try {
            var data = reqp.getRequestText().toString();
            var toCheck = mapper.deserialize(data, TargetVerificationInput.class);
            var matcher = SiteMatcherUtils.setupSites(List.of(toCheck.getTarget())).get(0);
            if (matcher.match(toCheck.getMatchAgainst())){
                respondOk(resp);
            }else{
                respondKo(resp,"Not matching",404);
            }
        }catch (Exception ex){
            respondKo(resp,ex.getMessage());
        }
    }
}
