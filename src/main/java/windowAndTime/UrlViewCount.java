package windowAndTime;

import java.sql.Timestamp;

/**
 * @author liuhuan
 * @date 2022-11-04
 */
public class UrlViewCount {
    public String url;
    public Long count;
    public Long windowStart;
    public Long windowEnd;
    public UrlViewCount() {
    }
    public UrlViewCount(String url, Long count, Long windowStart, Long windowEnd) {
        this.url = url;
        this.count = count;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
    }
    @Override
    public String toString() {
        return "UrlViewCount{" + "url='" + url + '\'' +
                ", count=" + count +
                ", windowStart=" + new Timestamp(windowStart) + ", windowEnd=" + new Timestamp(windowEnd) + '}';
    }

}
