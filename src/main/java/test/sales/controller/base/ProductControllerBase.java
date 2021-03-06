package test.sales.controller.base;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import test.sales.*;
import test.sales.bean.Product;
import test.sales.mask.ProductMask;
import test.sales.service.ProductService;

public abstract class ProductControllerBase {
  protected static final String path = "api/prod";
  protected static final String listPath = "api/prod_list";
  protected static final String attachmentPath = "api/prod_attachment";
  protected static final String pathKey = "/{imid}";

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = path + pathKey,
    method = RequestMethod.GET,
    produces = "application/json;charset=UTF-8"
  )
  public String get(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid,
      @RequestParam(required = false) String mask)
      throws Exception {
    ProductMask maskObj =
        mask == null || mask.equals("")
            ? new ProductMask().all(true)
            : new ProductMask(Long.valueOf(mask));
    Product bean = onGet(session, request, response, imid, maskObj);
    return jackson.writeValueAsString(bean);
  }

  protected abstract Product onGet(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Integer imid,
      ProductMask mask)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = path,
    method = RequestMethod.POST,
    produces = "application/json;charset=UTF-8"
  )
  public String add(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestBody Product bean)
      throws Exception {
    onAdd(session, request, response, bean);
    return jackson.writeValueAsString(bean);
  }

  protected abstract Product onAdd(
      HttpSession session, HttpServletRequest request, HttpServletResponse response, Product bean)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = path + pathKey,
    method = RequestMethod.PUT,
    produces = "application/json;charset=UTF-8"
  )
  public String update(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid,
      @RequestBody Product bean,
      @RequestParam(required = false) String mask)
      throws Exception {
    ProductMask maskObj =
        mask == null || mask.equals("")
            ? new ProductMask().all(true)
            : new ProductMask(Long.valueOf(mask));
    onUpdate(session, request, response, imid, bean, maskObj);
    return jackson.writeValueAsString(bean);
  }

  protected abstract Product onUpdate(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Integer imid,
      Product bean,
      ProductMask mask)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = path + pathKey,
    method = RequestMethod.DELETE,
    produces = "application/json;charset=UTF-8"
  )
  public void delete(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid)
      throws Exception {
    onDelete(session, request, response, imid);
  }

  protected abstract void onDelete(
      HttpSession session, HttpServletRequest request, HttpServletResponse response, Integer imid)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080", exposedHeaders = "Content-Range")
  @RequestMapping(
    value = listPath,
    method = {RequestMethod.GET, RequestMethod.POST},
    produces = "application/json;charset=UTF-8"
  )
  public String query(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @RequestParam(required = false, name = "filter") String paramFilter,
      @RequestBody(required = false) String bodyFilter,
      @RequestParam(required = false) String orderBy,
      @RequestHeader(required = false, name = "Range", defaultValue = "items=0-9") String range,
      @RequestParam(required = false) String mask)
      throws Exception {
    String filter = paramFilter == null || paramFilter.equals("") ? bodyFilter : paramFilter;
    FilterExpr filterObj =
        filter == null || filter.equals("") ? null : jackson.readValue(filter, FilterExpr.class);
    OrderByListExpr orderByListObj =
        orderBy == null || orderBy.equals("") ? null : OrderByListExpr.fromQuery(orderBy);
    RangeExpr rangeObj = RangeExpr.fromQuery(range);
    ProductMask maskObj =
        mask == null || mask.equals("")
            ? new ProductMask().all(true)
            : new ProductMask(Long.valueOf(mask));
    Long total = onCount(session, request, response, filterObj);
    if (total == null) return null;
    long start = rangeObj.getStart(total);
    long count = rangeObj.getCount(total);
    List<Product> results =
        onQuery(session, request, response, filterObj, orderByListObj, start, count, maskObj);
    response.setHeader(
        "Content-Range", RangeListExpr.getContentRange(start, results.size(), total));
    return jackson.writeValueAsString(results);
  }

  protected abstract Long onCount(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      FilterExpr filter)
      throws Exception;

  protected abstract List<Product> onQuery(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      FilterExpr filter,
      OrderByListExpr orderByList,
      long start,
      long count,
      ProductMask mask)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080", exposedHeaders = "Content-Disposition")
  @RequestMapping(
    value = attachmentPath + pathKey,
    method = RequestMethod.GET,
    produces = "application/octet-stream"
  )
  public void downloadAttachment(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid,
      @RequestParam(required = true) String name)
      throws Exception {
    InputStream input = inputStream(session, request, response, imid, name);
    if (input == null) return;
    String contentType = URLConnection.guessContentTypeFromName(name);
    response.setContentType(contentType == null ? "application/octet-stream" : contentType);
    response.setHeader("Content-Disposition", "attachment;fileName=" + name);
    StreamUtils.copy(input, response.getOutputStream());
    input.close();
    response.getOutputStream().close();
  }

  protected abstract InputStream inputStream(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Integer imid,
      String name)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = attachmentPath + pathKey,
    method = RequestMethod.PUT,
    produces = "application/json;charset=UTF-8"
  )
  public void uploadAttachment(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid,
      @RequestParam(required = true) String name)
      throws Exception {
    OutputStream output = outputStream(session, request, response, imid, name);
    if (output == null) return;
    StreamUtils.copy(request.getInputStream(), output);
    request.getInputStream().close();
    output.close();
  }

  protected abstract OutputStream outputStream(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Integer imid,
      String name)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = attachmentPath + pathKey,
    method = RequestMethod.DELETE,
    produces = "application/json;charset=UTF-8"
  )
  public void deleteAttachment(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid,
      @RequestParam(required = true) String name)
      throws Exception {
    onDeleteAttachment(session, request, response, imid, name);
  }

  protected abstract void onDeleteAttachment(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      Integer imid,
      String name)
      throws Exception;

  @CrossOrigin(origins = "http://192.168.100.43:8080")
  @RequestMapping(
    value = attachmentPath + pathKey,
    method = RequestMethod.GET,
    produces = "application/json;charset=UTF-8"
  )
  public String listAttachments(
      HttpSession session,
      HttpServletRequest request,
      HttpServletResponse response,
      @PathVariable Integer imid)
      throws Exception {
    List<String> result = onListAttachments(session, request, response, imid);
    return jackson.writeValueAsString(result);
  }

  protected abstract List<String> onListAttachments(
      HttpSession session, HttpServletRequest request, HttpServletResponse response, Integer imid)
      throws Exception;

  @Value(value = "#{jackson}")
  protected ObjectMapper jackson;

  @Value(value = "#{productService}")
  protected ProductService productService;
}
