package top.codings.websiphon.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class DateProcessUtils {

    private static String[] splits = {"-", "/", ".", "\\u4e00-\\u9fa5"};

    private static String[] cnDates = {"年","月","日"};

    public static Date process(String tryDate) {

        /*Date initDate = initOther(tryDate);
        if (initDate != null) {
            return initDate;
        }*/

        String date = null;
        String datePattern = null;
        String time = null;
        String timePattern;
        boolean chinaType = true;
        // 是否找到日期格式
        boolean findDate = true;

        Pattern pattern;
        Matcher matcher;

        // 中文格式的日期
        for (String split : splits) {
            boolean parse = true;
            datePattern = "y" + split + "M" + split + "d";
            // 找y-M-d
            pattern = Pattern.compile("\\d{4}[" + split + "]\\d{1,2}[" + split + "]\\d{1,2}[" + split + "]?");
            matcher = pattern.matcher(tryDate);
            if (matcher.find()) {
                date = matcher.group();
            } else {
                // 找M/d/y
                pattern = Pattern.compile("\\d{1,2}[" + split + "]\\d{1,2}[" + split + "]\\d{4}");
                matcher = pattern.matcher(tryDate);
                if (matcher.find()) {
                    date = matcher.group();
                    datePattern = "M" + split + "d" + split + "y";
                } else {
                    // 找y-M
                    pattern = Pattern.compile("\\d{4}[" + split + "]\\d{1,2}");
                    matcher = pattern.matcher(tryDate);
                    if (matcher.find()) {
                        date = matcher.group();
                        if (split.equals("\\u4e00-\\u9fa5")) {
                            date += "月01";
                        } else {
                            date += split + "01";
                        }
                    } else {
                        // 找M-d
                        pattern = Pattern.compile("[1-12][" + split + "][1-31]");
                        matcher = pattern.matcher(tryDate);
                        if (matcher.find()) {
                            date = matcher.group();
                            if (split.equals("\\u4e00-\\u9fa5")) {
                                date = Calendar.getInstance().get(Calendar.YEAR) + "年" + date;
                            } else {
                                date = Calendar.getInstance().get(Calendar.YEAR) + split + date;
                            }
                        } else {
                            parse = false;
                            datePattern = null;
                        }
                    }
                }
            }
            if (parse) {
                if (date.contains("年")) {
                    datePattern = "y年M月d";
                    if (date.contains("日")) {
                        datePattern = "y年M月d日";
                    }
                }
                break;
            }
        }
        if (StringUtils.isBlank(datePattern)) {
            chinaType = false;
            // 找国外日期格式
            pattern = Pattern.compile("[A-Z][a-z]{2,3} \\d{1,2}, \\d{4}");
            matcher = pattern.matcher(tryDate);
            if (matcher.find()) {
                date = matcher.group();
                datePattern = "MMM d, yyyy";
            } else {
                pattern = Pattern.compile("[A-Z][a-z]{2,3} \\d{1,2}");
                matcher = pattern.matcher(tryDate);
                if (matcher.find()) {
                    date = matcher.group();
                    date += ", " + Calendar.getInstance().get(Calendar.YEAR);
                    datePattern = "MMM d, yyyy";
                } else {
                    log.debug("解析日期格式无匹配项，尝试匹配邻近天数规则 >>>>>>>> {}", date);
                    findDate = false;
                    chinaType = true;
                }
            }
        }
        if (!findDate) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("y-M-d");
            datePattern = "y-M-d";
            Calendar cal = Calendar.getInstance();
            /*if (tryDate.contains("今天")) {
                date = simpleDateFormat.format(new Date());
            } else if (tryDate.contains("昨天")) {
                cal.add(Calendar.DATE, -1);
                date = simpleDateFormat.format(cal.getTime());
            } else if (tryDate.contains("前天")) {
                cal.add(Calendar.DATE, -2);
                date = simpleDateFormat.format(cal.getTime());
            } else if (tryDate.contains("大前天")) {
                cal.add(Calendar.DATE, -3);
                date = simpleDateFormat.format(cal.getTime());
            } else {
                return null;
            }*/
            return null;
        }

        //找时间
        pattern = Pattern.compile("(\\d{1,2}:\\d{1,2}:\\d{1,2})[ ](([a|A][m|M])|([p|P][m|M]))");
        matcher = pattern.matcher(tryDate);
        if (matcher.find()) {
            time = matcher.group();
            timePattern = "h:m:s aa";
            chinaType = false;
        } else {
            pattern = Pattern.compile("\\d{1,2}:\\d{1,2}:\\d{1,2}");
            matcher = pattern.matcher(tryDate);
            if (matcher.find()) {
                time = matcher.group();
                timePattern = "H:m:s";
            } else {
                pattern = Pattern.compile("(\\d{1,2}:\\d{1,2})[ ](([a|A][m|M])|([p|P][m|M]))");
                matcher = pattern.matcher(tryDate);
                if (matcher.find()) {
                    time = matcher.group();
                    timePattern = "h:m aa";
                } else {
                    pattern = Pattern.compile("(\\d{1,2}:\\d{1,2})");
                    matcher = pattern.matcher(tryDate);
                    if (matcher.find()) {
                        time = matcher.group();
                        timePattern = "H:m";
                    } else {
                        pattern = Pattern.compile("(\\d{1,2}[\\u4e00-\\u9fa5]\\d{1,2})");
                        matcher = pattern.matcher(tryDate);
                        if (matcher.find()) {
                            time = matcher.group();
                            time.replace("時", ":").replace("时", ":");
                            timePattern = "H:m";
                        } else {
                            timePattern = null;
                            log.debug("解析时间格式无匹配项 >>>>>>>>> {}", time);
                        }
                    }
                }
            }
        }

        SimpleDateFormat simpleDateFormat = null;
        if (chinaType) {
            if (StringUtils.isBlank(timePattern)) {
                simpleDateFormat = new SimpleDateFormat(datePattern);
            } else {
                try {
                    simpleDateFormat = new SimpleDateFormat(datePattern + " " + timePattern);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (StringUtils.isBlank(timePattern)) {
                simpleDateFormat = new SimpleDateFormat(datePattern, Locale.ENGLISH);
            } else {
                simpleDateFormat = new SimpleDateFormat(datePattern + " " + timePattern, Locale.ENGLISH);
            }
        }
        try {
            if (StringUtils.isBlank(time)) {
                return simpleDateFormat.parse(date);

            } else {
                return simpleDateFormat.parse(date + " " + time);

            }
        } catch (ParseException e) {
            log.error("解析日期时间出错 >>>>>>>>> 格式[{}] | 待解析值[{}]", datePattern + " " + timePattern, date + " " + time);
            try {
                simpleDateFormat.applyPattern(datePattern);
                return simpleDateFormat.parse(date);
            } catch (ParseException e1) {
                log.error("第二次解析日期出错 >>>>>>>>> 格式[{}] | 待解析值[{}]", datePattern , date );
            }
        }

        return null;
    }

    private static Date initOther(String tryDate) {
        if(isCnDate(tryDate)){
            return null;
        }
//        String regexBeforeHours = "^([\\d]+)[\\s]*([分钟小时天周个月年]{1,4})[前]*.*";
        String regexBeforeHours = ".*([\\d]+)[\\s ]*([分钟小时天周个月年]{1,4})[前]*.*";
        if (tryDate.matches(regexBeforeHours)) {
            int before = 0;
            Date result = new Date();
            try {
                String hours = tryDate.replaceFirst(regexBeforeHours, "$1");
                before = Integer.parseInt(hours);
            } catch (Throwable e) {
            }

            String mount = tryDate.replaceFirst(regexBeforeHours, "$2");
            if (mount.contains("分钟")) {
                result = DateUtils.addMinutes(new Date(), -before);
            } else if (mount.contains("小时")) {
                result = DateUtils.addHours(new Date(), -before);
            } else if (mount.contains("天")) {
                result = DateUtils.addDays(new Date(), -before);
            } else if (mount.contains("周")) {
                result = DateUtils.addWeeks(new Date(), -before);
            } else if (mount.contains("月")) {
                result = DateUtils.addMonths(new Date(), -before);
            } else if (mount.contains("年")) {
                result = DateUtils.addYears(new Date(), -before);
            }
            return result;
        }

        if (tryDate.contains("刚刚") || tryDate.contains("当前") || tryDate.contains("现在")) {
            return new Date();
        }
        return null;
    }

    private static boolean isCnDate(String tryDate){
        int count = 0;
        for (String cnDateStr : cnDates){
            if(tryDate.contains(cnDateStr)){
                count++;
            }
        }
        return count > 1 ? true : false;
    }

    private static boolean checkDate(String date,String datePattern){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        try {
            simpleDateFormat.parse(date);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) {
        String t = "2018-07-2812 09:32  2663 阅读";
       Date d =  process(t);
        System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d));
    }
}
