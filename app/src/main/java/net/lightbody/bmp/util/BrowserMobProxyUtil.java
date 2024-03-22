package net.lightbody.bmp.util;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;
import net.lightbody.bmp.core.har.HarPage;
import net.lightbody.bmp.mitm.exception.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrowserMobProxyUtil {
    private static final Logger log = LoggerFactory.getLogger(BrowserMobProxyUtil.class);
    private static final String VERSION_CLASSPATH_RESOURCE = "/net/lightbody/bmp/version";
    private static final String UNKNOWN_VERSION_STRING = "UNKNOWN-VERSION";

    private static final Supplier<String> version = Suppliers.memoize(() -> readVersionFileOnClasspath());

    public static String getVersionString() {
        return version.get();
    }

    public static Har copyHarThroughPageRef(Har har, String pageRef) {
        if (har == null || har.getLog() == null) {
            return new Har();
        }

        Set<String> pageRefsToCopy = new HashSet<>();
        boolean pageFound = false;

        List<HarPage> pages = har.getLog().getPages();
        for (HarPage page : pages) {
            pageRefsToCopy.add(page.getId());
            if (pageRef.equals(page.getId())) {
                pageFound = true;
                break;
            }
        }

        if (!pageFound) {
            return new Har();
        }

        HarLog logCopy = new HarLog();
        List<HarEntry> entries = har.getLog().getEntries();
        for (HarEntry entry : entries) {
            if (pageRefsToCopy.contains(entry.getPageref())) {
                logCopy.addEntry(entry);
            }
        }

        for (HarPage page : pages) {
            if (pageRefsToCopy.contains(page.getId())) {
                logCopy.addPage(page);
            }
        }

        Har harCopy = new Har(logCopy);
        return harCopy;
    }

    private static String readVersionFileOnClasspath() {
        try {
            return ClasspathResourceUtil.classpathResourceToString(VERSION_CLASSPATH_RESOURCE, StandardCharsets.UTF_8);
        } catch (UncheckedIOException e) {
            log.debug("Unable to load version from classpath resource: {}", VERSION_CLASSPATH_RESOURCE, e);
            return UNKNOWN_VERSION_STRING;
        }
    }
}
