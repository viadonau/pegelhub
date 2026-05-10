package at.pegelhub.connector.tstp.communication.impl;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import at.pegelhub.connector.tstp.service.TstpXmlService;
import at.pegelhub.connector.tstp.service.model.XmlQueryResponse;
import at.pegelhub.connector.tstp.service.model.XmlTsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import at.pegelhub.lib.model.Measurement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TstpCommunicatorImplTest {
	@Mock
	private HttpClient httpClient;
	@Mock
	private TstpXmlService tstpXmlService;
	@InjectMocks
	private TstpCommunicatorImpl tstpCommunicator;
	@Mock
	private HttpResponse<String> httpResponse;

	@BeforeEach
	public void setUp() {
		tstpCommunicator = new TstpCommunicatorImpl("localhost", 8080, httpClient, tstpXmlService);
	}

	@Test
	public void testGetMeasurements_returnsList() throws Exception {
		String zrid = "123";
		Instant readFrom = Instant.now();
		Instant readUntil = Instant.now();
		String responseBody = "test123";
		List<Measurement> measurements = new ArrayList<>();

		when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
		when(httpResponse.body()).thenReturn(responseBody);
		when(tstpXmlService.parseXmlGetResponseToMeasurements(responseBody)).thenReturn(measurements);

		List<Measurement> result = tstpCommunicator.getMeasurements(zrid, readFrom, readUntil);

		assertEquals(measurements, result);
		verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
		verify(tstpXmlService, times(1)).parseXmlGetResponseToMeasurements(responseBody);
	}

	@Test
	public void testGetMeasurements_returnsEmptyList() throws Exception {
		String zrid = "123";
		Instant readFrom = Instant.now();
		Instant readUntil = Instant.now();

		when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(new RuntimeException());

		List<Measurement> result = tstpCommunicator.getMeasurements(zrid, readFrom, readUntil);

		assertTrue(result.isEmpty());
		verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
		verify(tstpXmlService, never()).parseXmlGetResponseToMeasurements(anyString());
	}

	@Test
	public void testGetCatalog_returnsCatalog() throws Exception {
		int dbms = 1;
		String responseBody = "test123";
		XmlQueryResponse xmlQueryResponse = new XmlQueryResponse();

		when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
		when(httpResponse.body()).thenReturn(responseBody);
		when(tstpXmlService.parseXmlCatalog(responseBody)).thenReturn(xmlQueryResponse);

		XmlQueryResponse result = tstpCommunicator.getCatalog(dbms);

		assertEquals(xmlQueryResponse, result);
		verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
		verify(tstpXmlService, times(1)).parseXmlCatalog(responseBody);
	}

	@Test
	public void testGetCatalog_catalogIsNull_returnNull() throws Exception {
		int dbms = 1;

		when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(new RuntimeException());

		XmlQueryResponse result = tstpCommunicator.getCatalog(dbms);

		assertNull(result);
		verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
		verify(tstpXmlService, never()).parseXmlCatalog(anyString());
	}

	@Test
	public void testSendMeasurements_validRequest_successful() throws Exception {
		String zrid = "123";
		List<Measurement> measurements = new ArrayList<>();
		String requestBody = "test123";
		String responseBody = "response123";
		XmlTsResponse xmlTsResponse = new XmlTsResponse();
		xmlTsResponse.setMessage("confirm");

		when(tstpXmlService.parseXmlPutRequest(measurements)).thenReturn(requestBody);
		when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
		when(httpResponse.body()).thenReturn(responseBody);
		when(tstpXmlService.parseXmlPutResponse(responseBody)).thenReturn(xmlTsResponse);

		tstpCommunicator.sendMeasurements(zrid, measurements);

		verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
		verify(tstpXmlService, times(1)).parseXmlPutRequest(measurements);
		verify(tstpXmlService, times(1)).parseXmlPutResponse(responseBody);
	}

	@Test
	public void testSendMeasurements_invalidRequest_failure() throws Exception {
		String zrid = "123";
		List<Measurement> measurements = new ArrayList<>();
		String requestBody = "test123";

		when(tstpXmlService.parseXmlPutRequest(measurements)).thenReturn(requestBody);
		when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenThrow(new RuntimeException());

		tstpCommunicator.sendMeasurements(zrid, measurements);

		verify(httpClient, times(1)).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
		verify(tstpXmlService, times(1)).parseXmlPutRequest(measurements);
		verify(tstpXmlService, never()).parseXmlPutResponse(anyString());
	}}