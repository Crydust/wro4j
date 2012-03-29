/**
 *
 */
package ro.isdc.wro.model.resource.locator.support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.jmx.WroConfiguration;


/**
 * Test for {@link ServletContextUriLocator} class.
 *
 * @author Alex Objelean
 */
public class TestServletContextResourceLocator {
  private ServletContextResourceLocator locator;
  @Mock
  private ServletContext mockServletContext;
  @Mock
  private HttpServletRequest mockRequest;
  @Mock
  private HttpServletResponse mockResponse;
  @Mock
  private FilterConfig mockFilterConfig;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(mockFilterConfig.getServletContext()).thenReturn(mockServletContext);
    final WroConfiguration config = new WroConfiguration();
    config.setConnectionTimeout(100);
    Context.set(Context.webContext(mockRequest, mockResponse, mockFilterConfig), config);
  }

  @Test(expected = NullPointerException.class)
  public void cannotAcceptNullUri()
      throws Exception {
    locator = new ServletContextResourceLocator(mockServletContext, null);
  }

  @Test
  public void shouldAcceptNullServletContext()
      throws Exception {
    locator = new ServletContextResourceLocator(mockServletContext, "");
    Assert.assertNotNull(locator);
  }

  @Test
  public void testWildcard1Resources()
      throws IOException {
    locator = new ServletContextResourceLocator(mockServletContext, createUri("/css/*.css"));
    Assert.assertNotNull(locator.getInputStream());
  }

  @Test
  public void testWildcard2Resources()
      throws IOException {
    locator = new ServletContextResourceLocator(mockServletContext, createUri("/css/*.cs?"));
    Assert.assertNotNull(locator.getInputStream());
  }

  @Test
  public void testWildcard3Resources()
      throws IOException {
    locator = new ServletContextResourceLocator(mockServletContext, createUri("/css/*.???"));
    Assert.assertNotNull(locator.getInputStream());
  }

  @Test
  public void testRecursiveWildcardResources()
      throws IOException {
    locator = new ServletContextResourceLocator(mockServletContext, createUri("/css/**.css"));
    Assert.assertNotNull(locator.getInputStream());
  }

  @Test
  public void testWildcardInexistentResources()
      throws IOException {
    locator = new ServletContextResourceLocator(mockServletContext, createUri("/css/**.NOTEXIST"));
    Assert.assertNotNull(locator.getInputStream());
  }

  private String createUri(final String uri)
      throws IOException {
    final URL url = Thread.currentThread().getContextClassLoader().getResource("ro/isdc/wro/model/resource/locator/");
    Mockito.when(mockServletContext.getRealPath(Mockito.anyString())).thenReturn(url.getPath());
    // Mockito.when(Context.get().getServletContext().getRequestDispatcher(Mockito.anyString())).thenReturn(null);
    return uri;
  }

  @Test(expected = IOException.class)
  public void testSomeUri()
      throws Exception {
    locator = new ServletContextResourceLocator(mockServletContext, createUri("resourcePath"));
    locator.getInputStream();
  }

  /**
   * Make this test method to follow a flow which throws IOException
   */
  @Test(expected = IOException.class)
  public void testInvalidUrl()
      throws Exception {
    Mockito.when(mockServletContext.getResourceAsStream(Mockito.anyString())).thenReturn(null);
    Mockito.when(mockServletContext.getRequestDispatcher(Mockito.anyString())).thenReturn(null);

    locator = new ServletContextResourceLocator(mockServletContext, "/css/resourcePath.css");
    final InputStream is = locator.getInputStream();
    // the response should be empty
    Assert.assertEquals(-1, is.read());
  }

  /**
   * Simulates a resource which redirects to some valid location.
   */
  @Test
  public void testRedirectingResource()
      throws Exception {
    Mockito.when(mockServletContext.getResourceAsStream(Mockito.anyString())).thenReturn(null);
    Mockito.when(mockServletContext.getRequestDispatcher(Mockito.anyString())).thenReturn(null);
    final InputStream is = simulateRedirectWithLocation("http://www.google.com");
    Assert.assertNotSame(-1, is.read());
  }

  /**
   * Simulates a resource which redirects to some valid location.
   */
  @Test(expected = IOException.class)
  public void testRedirectingResourceToInvalidLocation()
      throws Exception {
    Mockito.when(mockServletContext.getResourceAsStream(Mockito.anyString())).thenReturn(null);
    Mockito.when(mockServletContext.getRequestDispatcher(Mockito.anyString())).thenReturn(null);
    simulateRedirectWithLocation("http://INVALID/");
  }

  @Test
  public void shouldPreferServletContextBasedResolving()
      throws IOException {
    final InputStream is = new ByteArrayInputStream("a {}".getBytes());
    Mockito.when(Context.get().getServletContext().getResourceAsStream(Mockito.anyString())).thenReturn(is);

    locator = new ServletContextResourceLocator(mockServletContext, "test.css");
    locator.setLocatorStrategy(ServletContextResourceLocator.LocatorStrategy.SERVLET_CONTEXT_FIRST);

    final InputStream actualIs = locator.getInputStream();

    final BufferedReader br = new BufferedReader(new InputStreamReader(actualIs));
    Assert.assertEquals("a {}", br.readLine());
  }

  private InputStream simulateRedirectWithLocation(final String location)
      throws IOException {
    final RequestDispatcher requestDispatcher = new RequestDispatcher() {
      public void include(final ServletRequest request, final ServletResponse response)
          throws ServletException, IOException {
        final HttpServletResponse res = (HttpServletResponse) response;
        // valid resource
        res.sendRedirect(location);
      }

      public void forward(final ServletRequest request, final ServletResponse response)
          throws ServletException, IOException {
        throw new UnsupportedOperationException();
      }
    };

    Mockito.when(mockRequest.getRequestDispatcher(Mockito.anyString())).thenReturn(requestDispatcher);
    locator = new ServletContextResourceLocator(mockServletContext, "/doesntMatter");
    return locator.getInputStream();
  }

  @Test(expected = NullPointerException.class)
  public void cannotSetNullLocatorStrategy() {
    locator = new ServletContextResourceLocator(mockServletContext, "/doesntMatter");
    locator.setLocatorStrategy(null);
  }

  @After
  public void resetContext() {
    Context.unset();
  }
}