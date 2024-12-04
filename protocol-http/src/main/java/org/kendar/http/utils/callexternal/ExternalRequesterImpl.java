package org.kendar.http.utils.callexternal;


import com.networknt.schema.format.InetAddressValidator;
import org.kendar.apis.base.Request;
import org.kendar.apis.base.Response;
import org.kendar.apis.converters.RequestResponseBuilder;
import org.kendar.http.utils.ConnectionBuilder;
import org.kendar.http.utils.dns.DnsMultiResolver;

public class ExternalRequesterImpl extends BaseRequesterImpl implements ExternalRequester {

    public ExternalRequesterImpl(RequestResponseBuilder requestResponseBuilder, DnsMultiResolver multiResolver,
                                 ConnectionBuilder connectionBuilder) {
        super(requestResponseBuilder, multiResolver, connectionBuilder);
    }

    @Override
    public void callSite(Request request, Response response)
            throws Exception {
        var resolved = multiResolver.resolveRemote(request.getHost());
        if (resolved.isEmpty()) {
            if (!InetAddressValidator.getInstance().isValidInet4Address(request.getHost())) {
                response.setStatusCode(404);
                return;
            }
        }
        super.callSite(request, response);
    }
}
