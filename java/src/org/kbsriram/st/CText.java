package org.kbsriram.st;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CText
{
    public final static CText parse(File file)
        throws IOException
    {
        LineNumberReader lnr = new LineNumberReader
            (new FileReader(file));

        int curline = lnr.getLineNumber();
        Block curblock = readBlock(lnr);
        if (curblock.getType() != Block.Type.DATE) {
            throw error(file, curline, "Need date");
        }
        Date date = asDate(file, curline, curblock.getContent());

        curline = lnr.getLineNumber();
        curblock = readBlock(lnr);
        if (curblock.getType() != Block.Type.TITLE) {
            throw error(file, curline, "Need title");
        }
        String title = curblock.getContent();

        List<Block> paras = new ArrayList<Block>();
        curline = lnr.getLineNumber();
        while ((curblock = readBlock(lnr)) != null) {
            if ((curblock.getType() == Block.Type.DATE) ||
                (curblock.getType() == Block.Type.TITLE)) {
                throw error
                    (file, curline, "Unexpected block: "+curblock.getType());
            }
            paras.add(curblock);
            curline = lnr.getLineNumber();
        }
        lnr.close();
        return new CText(date, title, paras);
    }

    public final Date getDate()
    { return m_date; }

    public final String getTitle()
    { return m_title; }

    public final List<Block> getParagraphs()
    { return m_paras; }

    public final void writeArticle(File tplroot, File dstroot)
        throws IOException
    {
        File dst = new File
            (dstroot, makeFileNameFrom(m_title, m_date));
        File check = dst.getParentFile();
        if (!check.isDirectory()) { check.mkdirs(); }

        PrintWriter pw =
            new PrintWriter
            (new BufferedWriter
             (new FileWriter(dst)));

        Map<String,String> vars = new HashMap<String,String>();
        vars.put("title", htmlEscape(m_title));
        vars.put("date", niceDate(m_date));
        vars.put("filename", dst.getName());
        vars.put("iso8601", iso8601(m_date));
        cat(new File(tplroot, "article_header.txt"), pw, vars);

        pw.println("<div class=\"e-content entry-content\">");
        for (Block para: m_paras) {
            writeParagraph(para, pw);
        }
        pw.println("</div>");

        cat(new File(tplroot, "footer.txt"), pw, null);
        pw.close();
    }

    public final static String makeFileNameFrom
        (String title, Date d)
    {
        StringBuilder sb = new StringBuilder(s_filedir_date.format(d));
        char[] chars = title.toCharArray();
        boolean inws = false;
        for (int i=0; i<chars.length; i++) {
            char cur = Character.toLowerCase(chars[i]);
            if (((cur >= 'a') && (cur <= 'z')) ||
                ((cur >= '0') && (cur <= '9'))) {
                inws = false;
                sb.append(cur);
                continue;
            }
            if (Character.isWhitespace(cur)) {
                if (!inws) {
                    inws = true;
                    sb.append("-");
                }
                continue;
            }
            // just ignore anything else.
        }
        sb.append(".html");
        return sb.toString();
    }

    public final static void writeParagraph(Block b, PrintWriter pw)
    { writeParagraph(b, pw, ""); }

    public final static void writeParagraph
        (Block b, PrintWriter pw, String pattrs)
    {
        switch (b.getType()) {
        case CODE:
            pw.print("<pre "+pattrs+">");
            break;
        case PARAGRAPH:
            pw.print("<p "+pattrs+">");
            break;
        case ASIS:
            break;
        default:
            throw new IllegalStateException("Unexpected tag: "+b.getType());
        }

        if (b.getType() == Block.Type.CODE) {
            pw.print(htmlEscape(b.getContent()));
        }
        else { pw.print(b.getContent()); }

        switch (b.getType()) {
        case CODE:
            pw.println("</pre>");
            break;
        case PARAGRAPH:
            pw.println("</p>");
            break;
        case ASIS:
            break;
        default:
            throw new IllegalStateException("Unexpected tag: "+b.getType());
        }
    }

    public final static String niceDate(Date d)
    { return s_nicedate.format(d); }
        
    public final static String iso8601(Date d)
    { return s_sdf.format(d); }
        
    public final static String htmlEscape(String line)
    {
        StringBuilder sb = new StringBuilder();
        char[] chars = line.toCharArray();
        for (int i=0; i<chars.length; i++) {
            char cur = chars[i];
            if ((cur == '\n') || (cur == '\r') || (cur == '\t')) {
                sb.append(cur);
            }
            else if ((cur >= ' ') && (cur <= '~') &&
                (cur != '<') && (cur != '>') && (cur != '&')) {
                sb.append(cur);
            }
            else {
                sb.append("&#");
                sb.append(Integer.toString(cur));
                sb.append(";");
            }
        }
        return sb.toString();
    }

    public final static void cat
        (File src, PrintWriter pw, Map<String,String> params)
        throws IOException
    {
        BufferedReader br = new BufferedReader
            (new FileReader(src));
        String line;
        boolean first = true;
        while ((line = br.readLine()) != null) {
            pw.println(replace(line, params));
        }
        br.close();
    }

    private final static String replace
        (String line, Map<String,String> vars)
    {
        Matcher m = s_var_pattern.matcher(line);
        StringBuilder sb = new StringBuilder();
        int cur = 0;
        while (m.find()) {
            sb.append(line.substring(cur, m.start()));
            String v = vars.get(m.group(1));
            if (v == null) {
                throw new IllegalArgumentException
                    ("Missing '"+m.group(1)+"' in "+line);
            }
            sb.append(v);
            cur = m.end();
        }
        if (cur < line.length()) {
            sb.append(line.substring(cur));
        }
        return sb.toString();
    }

    private final static IOException error(File f, int ln, String msg)
    { return new IOException(f.getName()+": "+(ln+1)+": "+msg); }

    private final static IOException error(File f, int ln, Throwable cause)
    {
        return
            new IOException
            (f.getName()+": "+(ln+1)+": "+cause.getMessage(), cause);
    }

    private final static Block readBlock(LineNumberReader lnr)
        throws IOException
    {
        String line;

        // skip empty lines
        while ((line = endTrim(lnr.readLine())) != null) {
            if (line.length() == 0) {
                continue;
            }
            else {
                break;
            }
        }
        if (line == null) { return null; }

        Block.Type bt;
        StringBuilder sb = new StringBuilder();
        if (line.equals("$date")) { bt = Block.Type.DATE; }
        else if (line.equals("$title")) { bt = Block.Type.TITLE; }
        else if (line.equals("$asis")) { bt = Block.Type.ASIS; }
        else if (line.equals("$code_begin")) { bt = Block.Type.CODE; }
        else {
            bt = Block.Type.PARAGRAPH;
            sb.append(line);
        }

        while ((line = endTrim(lnr.readLine())) != null) {
            if ((bt == Block.Type.CODE) &&
                (line.equals("$code_end"))) {
                break;
            }
            if ((bt != Block.Type.CODE) &&
                (line.length() == 0)) {
                break;
            }
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(line);
        }

        return new Block(bt, sb.toString());
    }

    private final static String endTrim(String in)
    {
        if (in == null) { return null; }
        int end = in.length();
        while ((end > 0) && (in.charAt(end-1) == ' ')) {
            end--;
        }
        if (end == in.length()) { return in; }
        return in.substring(0, end);
    }

    private final static Date asDate(File f, int ln, String text)
        throws IOException        
    {
        try { return s_sdf.parse(text); }
        catch (ParseException pe) {
            throw error(f, ln, pe);
        }
    }

    private CText(Date date, String title, List<Block> paras)
    {
        m_date = date;
        m_title = title;
        m_paras = paras;
    }

    private final Date m_date;
    private final String m_title;
    private final List<Block> m_paras;

    private final static SimpleDateFormat s_sdf =
        new SimpleDateFormat("yyyy-MM-dd");
    private final static SimpleDateFormat s_nicedate =
        new SimpleDateFormat("MMMM d, yyyy");
    private final static SimpleDateFormat s_filedir_date =
        new SimpleDateFormat("yyyy/MM/");
    private final static Pattern s_var_pattern =
        Pattern.compile("\\$\\{(\\w+)\\}");

    public final static class Block
    {
        public enum Type { DATE, TITLE, CODE, PARAGRAPH, ASIS };
        private Block(Type t, String c)
        {
            m_type = t;
            m_content = c;
        }
        public Type getType()
        { return m_type; }
        public String getContent()
        { return m_content; }
        private final Type m_type;
        private final String m_content;
    }
}
