package org.apache.poi.xwpf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;

import javax.crypto.Cipher;

import org.apache.poi.POIDataSamples;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionHeader;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.Assume;
import org.junit.Test;

public class TestXWPFBugs {
    /**
     * A word document that's encrypted with non-standard
     *  Encryption options, and no cspname section. See bug 53475
     */
    @Test
    public void bug53475NoCSPName() throws Exception {
        File file = POIDataSamples.getDocumentInstance().getFile("bug53475-password-is-solrcell.docx");
        NPOIFSFileSystem filesystem = new NPOIFSFileSystem(file, true);

        // Check the encryption details
        EncryptionInfo info = new EncryptionInfo(filesystem);
        assertEquals(128, info.getHeader().getKeySize());
        assertEquals(EncryptionHeader.ALGORITHM_AES_128, info.getHeader().getAlgorithm());
        assertEquals(EncryptionHeader.HASH_SHA1, info.getHeader().getHashAlgorithm());

        // Check it can be decoded
        Decryptor d = Decryptor.getInstance(info);		
        assertTrue("Unable to process: document is encrypted", d.verifyPassword("solrcell"));

        // Check we can read the word document in that
        InputStream dataStream = d.getDataStream(filesystem);
        OPCPackage opc = OPCPackage.open(dataStream);
        XWPFDocument doc = new XWPFDocument(opc);
        XWPFWordExtractor ex = new XWPFWordExtractor(doc);
        String text = ex.getText();
        assertNotNull(text);
        assertEquals("This is password protected Word document.", text.trim());
        ex.close();
    }

    /**
     * A word document with aes-256, i.e. aes is always 128 bit (= 128 bit block size),
     * but the key can be 128/192/256 bits
     */
    @Test
    public void bug53475_aes256() throws Exception {
        int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
        Assume.assumeTrue("Please install JCE Unlimited Strength Jurisdiction Policy files for AES 256", maxKeyLen == 2147483647);

        File file = POIDataSamples.getDocumentInstance().getFile("bug53475-password-is-pass.docx");
        NPOIFSFileSystem filesystem = new NPOIFSFileSystem(file, true);

        // Check the encryption details
        EncryptionInfo info = new EncryptionInfo(filesystem);
        assertEquals(16, info.getHeader().getBlockSize());
        assertEquals(256, info.getHeader().getKeySize());
        assertEquals(EncryptionHeader.ALGORITHM_AES_256, info.getHeader().getAlgorithm());
        assertEquals(EncryptionHeader.HASH_SHA1, info.getHeader().getHashAlgorithm());

        // Check it can be decoded
        Decryptor d = Decryptor.getInstance(info);		
        assertTrue("Unable to process: document is encrypted", d.verifyPassword("pass"));

        // Check we can read the word document in that
        InputStream dataStream = d.getDataStream(filesystem);
        OPCPackage opc = OPCPackage.open(dataStream);
        XWPFDocument doc = new XWPFDocument(opc);
        XWPFWordExtractor ex = new XWPFWordExtractor(doc);
        String text = ex.getText();
        assertNotNull(text);
        // I know ... a stupid typo, maybe next time ...
        assertEquals("The is a password protected document.", text.trim());
        ex.close();
    }
}
