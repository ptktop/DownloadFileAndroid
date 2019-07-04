package com.ptktop.downloadfileandroid.manager;

import android.os.Environment;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/*********************
 * Created by PTKTOP *
 *********************/

public class DownloadFileAll {

    private DownloadListener listener;
    private String url;
    private String fileTarget;
    private String fileName;

    public DownloadFileAll(DownloadListener listener, String url, String fileTarget, String fileName) {
        this.listener = listener;
        this.url = url;
        this.fileTarget = fileTarget;
        this.fileName = fileName;
    }

    public void run() {
        getDownloadObservable
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(downloadObserver);
    }

    private Observable<Response> getDownloadObservable = Observable.fromCallable(new Callable<Response>() {
        @Override
        public okhttp3.Response call() throws Exception {
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            return setUpOkHttp().newCall(request).execute();
        }
    }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());

    private Observer<Response> downloadObserver = new Observer<okhttp3.Response>() {
        @Override
        public void onSubscribe(Disposable d) {
        }

        @Override
        public void onNext(okhttp3.Response response) {
            if (response.isSuccessful()) {
                storageFile(response.body());
            } else {
                listener.storageFileComplete(false,"On Next >>> " + response.body());
            }
        }

        @Override
        public void onError(Throwable e) {
            listener.storageFileComplete(false,"On Error >>> " + e.getMessage());
        }

        @Override
        public void onComplete() {
            listener.storageFileComplete(true,"Complete");
        }
    };

    private OkHttpClient setUpOkHttp() {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.MINUTES)
                .connectTimeout(30, TimeUnit.MINUTES)
                .addInterceptor(interceptor)
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return originalResponse.newBuilder()
                            .body(new DownloadResponseBody(originalResponse.body(), listener))
                            .build();
                })
                .build();
    }

    private void storageFile(@Nullable ResponseBody responseBody) {
        try {
            if (responseBody != null) {
                File file = new File(Environment.getExternalStorageDirectory() + fileTarget, fileName);
                BufferedSink sink = Okio.buffer(Okio.sink(file));
                sink.writeAll(responseBody.source());
                sink.close();
                listener.storageFileComplete(true, "Complete");
            }
        } catch (InterruptedIOException e) {
            listener.storageFileComplete(false, "InterruptedIOException >>> " + e.getMessage());
        } catch (IOException e) {
            listener.storageFileComplete(false, "IOException >>> " + e.getMessage());
        }
    }

    private class DownloadResponseBody extends ResponseBody {

        private ResponseBody responseBody;
        private DownloadListener downloadListener;
        private BufferedSource bufferedSource;

        private DownloadResponseBody(ResponseBody responseBody, DownloadListener downloadListener) {
            this.responseBody = responseBody;
            this.downloadListener = downloadListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @NotNull
        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(@NotNull Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    // read() returns the number of bytes read, or -1 if this source is exhausted.
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    downloadListener.downloadProcess(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    public interface DownloadListener {
        void downloadProcess(long bytesRead, long contentLength, boolean done);

        void storageFileComplete(boolean complete, String message);
    }
}
