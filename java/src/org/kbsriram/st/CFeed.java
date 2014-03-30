package org.kbsriram.st;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class CFeed
{
    public CFeed() {}

    public void addEntryFrom(CText text)
    {
        Date d = text.getDate();
        String title = text.getTitle();
        CText.Block lead = text.getParagraphs().get(0);
        m_entries.add(new Entry(d, title, lead));
    }

    public TreeSet<Entry> getEntries()
    { return m_entries; }

    public void writeFeed(File tplroot, File dstroot)
        throws IOException
    {
        int entryid = 0;
        int maxpagenum = m_entries.size() % ENTRIES_PER_PAGE + 1;
        PrintWriter pw = null;
        Map<String,String> vars = new HashMap<String,String>();

        for (Entry entry: m_entries) {
            if ((entryid % ENTRIES_PER_PAGE) == 0) {
                int pagenum = (entryid / ENTRIES_PER_PAGE);
                if (pw != null) {
                    closePage(pagenum-1, maxpagenum, tplroot, pw);
                }
                File dst;
                if (pagenum == 0) { dst = new File(dstroot, "index.html"); }
                else { dst = new File(dstroot, pagenum+".html"); }
                pw =
                    new PrintWriter
                    (new BufferedWriter
                     (new FileWriter(dst)));
                vars.put("title", "KB Sriram");
                vars.put("filename", dst.getName());
                if (pagenum == 0) {
                    vars.put("header", "KB Sriram");
                }
                else {
                    vars.put
                        ("header", "<a href=\"/\">KB Sriram</a>");
                }
                CText.cat(new File(tplroot, "feed_header.txt"), pw, vars);
            }
            writeEntry(tplroot, entry, pw);
            entryid++;
        }
        if (pw != null) {
            closePage(maxpagenum, maxpagenum, tplroot, pw);
        }
    }

    private final static void writeEntry
        (File tplroot, Entry entry, PrintWriter pw)
        throws IOException
    {
        pw.println("<div class=\"h-entry hentry\">");
        Map<String,String> vars = new HashMap<String,String>();
        vars.put("date", CText.niceDate(entry.getDate()));
        vars.put("iso8601", CText.iso8601(entry.getDate()));
        vars.put("title", CText.htmlEscape(entry.getTitle()));
        vars.put
            ("title_url", CText.makeFileNameFrom
             (entry.getTitle(), entry.getDate()));
        CText.cat(new File(tplroot, "entry_header.txt"), pw, vars);
        CText.writeParagraph(entry.getLead(), pw, "class=\"p-summary\"");
        pw.println("<div class=\"spacer\"></div>");
        pw.println("</div>");
    }

    private final static void closePage
        (int curpage, int maxpage, File tplroot, PrintWriter pw)
            throws IOException
    {
        Map<String,String> vars = new HashMap<String,String>();

        vars.put("page_older", linkTo(curpage+1, maxpage, "Older", "prev"));
        vars.put("page_newer", linkTo(curpage-1, maxpage, "Newer", "next"));
        CText.cat(new File(tplroot, "feed_footer.txt"), pw, vars);
        CText.cat(new File(tplroot, "footer.txt"), pw, null);
        pw.close();
    }

    private final static String linkTo
        (int targetpage, int maxpage, String m, String rel)
    {
        if (targetpage < 0) { return ""; }
        if (targetpage > maxpage) { return ""; }

        if (targetpage == 0) {
            return "<a rel=\""+rel+"\" href=\"index.html\">"+m+"</a>";
        }
        else {
            return "<a rel=\""+rel+"\" href=\""+targetpage+".html\">"+m+"</a>";
        }
    }

    private final TreeSet<Entry> m_entries = new TreeSet<Entry>();
    private final static int ENTRIES_PER_PAGE = 5;

    public final static class Entry
        implements Comparable<Entry>        
    {
        private Entry(Date d, String title, CText.Block lead)
        {
            m_date = d;
            m_title = title;
            m_lead = lead;
        }
        public int compareTo(Entry y)
        { return y.m_date.compareTo(m_date); }

        @Override
        public boolean equals(Object o)
        {
            if (!(o instanceof Entry)) { return false; }
            Entry other = (Entry) o;
            return m_date.equals(other.m_date);
        }

        public Date getDate()
        { return m_date; }

        public String getTitle()
        { return m_title; }

        public CText.Block getLead()
        { return m_lead; }

        private final Date m_date;
        private final String m_title;
        private final CText.Block m_lead;
    }
}

