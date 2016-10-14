package com.fsck.k9.preferences;

import com.fsck.k9.Account;
import com.fsck.k9.Preferences;
import com.fsck.k9.mail.AuthType;

import org.apache.tools.ant.filters.StringInputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("unchecked")
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class SettingsImporterTest {

    @Before
    public void before() {
        deletePreExistingAccounts();
    }

    private void deletePreExistingAccounts() {
        Preferences preferences = Preferences.getPreferences(RuntimeEnvironment.application);
        Collection<Account> availableAccounts =
                preferences.getAvailableAccounts();
        for(Account account: availableAccounts) {
            preferences.deleteAccount(account);
        }
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnBlankFile() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnMissingFormat() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings version=\"1\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnInvalidFormat() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings version=\"1\" format=\"A\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnNonPositiveFormat() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings version=\"1\" format=\"0\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnMissingVersion() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnInvalidVersion() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"A\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test(expected = SettingsImportExportException.class)
    public void importSettings_throwsExceptionOnNonPositiveVersion() throws SettingsImportExportException {
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"0\"></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        SettingsImporter.importSettings(RuntimeEnvironment.application, inputStream, true, accountUuids, true);
    }

    @Test
    public void parseSettings_account() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\""+validUUID+"\"><name>Account</name></account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add("1");
        SettingsImporter.Imported results = SettingsImporter.parseSettings(inputStream, true, accountUuids, true);
        assertEquals(1, results.accounts.size());
        assertEquals("Account", results.accounts.get(validUUID).name);
        assertEquals(validUUID, results.accounts.get(validUUID).uuid);
    }

    @Test
    public void parseSettings_account_xoauth2() throws SettingsImportExportException {

        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\""+validUUID+"\"><name>Account</name>" +
                "<incoming-server><authentication-type>XOAUTH2</authentication-type></incoming-server>" +
                "</account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add(validUUID);
        SettingsImporter.Imported results = SettingsImporter.parseSettings(inputStream, true, accountUuids, false);
        assertEquals("Account", results.accounts.get(validUUID).name);
        assertEquals(validUUID, results.accounts.get(validUUID).uuid);
        assertEquals(AuthType.XOAUTH2, results.accounts.get(validUUID).incoming.authenticationType);
    }

    @Test
    public void importSettings_doesNotDisableXoauth2Accounts() throws SettingsImportExportException {
        String validUUID = UUID.randomUUID().toString();
        InputStream inputStream = new StringInputStream("<k9settings format=\"1\" version=\"1\">" +
                "<accounts><account uuid=\""+validUUID+"\"><name>Account</name>" +
                "<incoming-server type=\"IMAP\">" +
                    "<connection-security>SSL_TLS_REQUIRED</connection-security>" +
                    "<username>user@gmail.com</username>" +
                    "<authentication-type>XOAUTH2</authentication-type>" +
                    "<host>googlemail.com</host>" +
                "</incoming-server>" +
                "<outgoing-server type=\"SMTP\">" +
                    "<connection-security>SSL_TLS_REQUIRED</connection-security>" +
                    "<username>user@googlemail.com</username>" +
                    "<authentication-type>XOAUTH2</authentication-type>" +
                    "<host>googlemail.com</host>" +
                "</outgoing-server>" +
                "<settings><value key=\"a\">b</value></settings>" +
                "<identities><identity><email>user@gmail.com</email></identity></identities>" +
                "</account></accounts></k9settings>");
        List<String> accountUuids = new ArrayList<>();
        accountUuids.add(validUUID);
        
        SettingsImporter.ImportResults results = SettingsImporter.importSettings(
                RuntimeEnvironment.application, inputStream, true, accountUuids, false);

        assertEquals(0, results.errorneousAccounts.size());
        assertEquals(1, results.importedAccounts.size());
        assertEquals("Account", results.importedAccounts.get(0).imported.name);
        assertEquals(validUUID, results.importedAccounts.get(0).imported.uuid);

        assertTrue(Preferences.getPreferences(RuntimeEnvironment.application)
                .getAccount(validUUID).isEnabled());
    }

}
