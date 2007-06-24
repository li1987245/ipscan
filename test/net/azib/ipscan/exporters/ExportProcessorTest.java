/**
 * 
 */
package net.azib.ipscan.exporters;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;

import net.azib.ipscan.config.Labels;
import net.azib.ipscan.core.ScanningResult;
import net.azib.ipscan.core.ScanningResultList;
import net.azib.ipscan.exporters.ExportProcessor.ScanningResultSelector;
import net.azib.ipscan.fetchers.Fetcher;
import net.azib.ipscan.fetchers.FetcherRegistry;
import net.azib.ipscan.fetchers.IPFetcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * ExportProcessorTest
 *
 * @author anton
 */
public class ExportProcessorTest {
	
	private FetcherRegistry fetcherRegistry;
	
	@Before
	public void setUp() {
		fetcherRegistry = createMock(FetcherRegistry.class);
		expect(fetcherRegistry.getSelectedFetchers())
			.andReturn(Collections.<Fetcher>singletonList(new IPFetcher())).anyTimes();
		replay(fetcherRegistry);
	}
	
	@After
	public void tearDown() {
		verify(fetcherRegistry);
	}

	@Test
	public void testProcess() throws Exception {
		File file = File.createTempFile("exportTest", "txt");
		ExportProcessor exportProcessor = new ExportProcessor(new TXTExporter(), file.getAbsolutePath());
		
		ScanningResultList scanningResultList = new ScanningResultList(fetcherRegistry);
		scanningResultList.add(InetAddress.getByName("192.168.0.13"));
		exportProcessor.process(scanningResultList, "megaFeeder", null);
		
		String content = readFileContent(file);
		
		assertTrue(content.indexOf("megaFeeder") > 0);
		assertTrue(content.indexOf(Labels.getLabel(new IPFetcher().getLabel())) > 0);
		assertTrue(content.indexOf("fooBar") < 0);		
		assertTrue(content.indexOf("192.168.0.13") > 0);		
	}

	/**
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private String readFileContent(File file) throws FileNotFoundException, IOException {
		StringBuffer buffer = new StringBuffer();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		while((line = reader.readLine()) != null) {
			buffer.append(line);
		}
		reader.close();
		return buffer.toString();
	}
	
	@Test
	public void testProcessWithSelector() throws Exception {
		File file = File.createTempFile("exportTest", "txt");
		ExportProcessor exportProcessor = new ExportProcessor(new TXTExporter(), file.getAbsolutePath());
		
		ScanningResultList scanningResultList = new ScanningResultList(fetcherRegistry);
		
		scanningResultList.add(InetAddress.getByName("192.168.13.66"));
		scanningResultList.add(InetAddress.getByName("192.168.13.67"));
		scanningResultList.add(InetAddress.getByName("192.168.13.76"));
		
		exportProcessor.process(scanningResultList, "feeder2", new ScanningResultSelector() {
			public boolean isResultSelected(int index, ScanningResult result) {
				// select only IP addresses ending with 6
				return ((String)result.getValues().get(0)).endsWith("6");
			}
		});
		
		String content = readFileContent(file);
		
		assertTrue(content.indexOf("feeder2") > 0);
		assertTrue(content.indexOf("192.168.13.66") > 0);
		assertTrue(content.indexOf("192.168.13.67") < 0);		
		assertTrue(content.indexOf("192.168.13.76") > 0);		
	}

}