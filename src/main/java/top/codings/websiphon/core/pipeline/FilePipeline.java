package top.codings.websiphon.core.pipeline;

import lombok.extern.slf4j.Slf4j;
import top.codings.websiphon.bean.BasicWebRequest;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class FilePipeline extends ReadWritePipelineAdapter<BasicWebRequest, String> {
    protected String filePath;
    protected String charset;
    protected ExecutorService executorService;

    public FilePipeline(String filePath, String charset) {
        this.filePath = filePath;
        this.charset = charset;
    }

    @Override
    public void init() {
        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))))) {
                String line;
                List<BasicWebRequest> out = new LinkedList<>();
                while ((line = br.readLine()) != null) {
                    out.clear();
                    conversion(line, out);
                    for (BasicWebRequest basicWebRequest : out) {
                        queue.transfer(basicWebRequest);
//                        write(basicWebRequest);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("读取文件异常");
            } catch (InterruptedException e) {
                return;
            }
        });

    }

    @Override
    public void conversion(String param, List<BasicWebRequest> out) {
        BasicWebRequest request = new BasicWebRequest();
        request.setUri(param);
        out.add(request);
    }

    @Override
    public void close() throws Exception {
        super.close();
        if (null != executorService) {
            executorService.shutdownNow();
        }
    }
}
