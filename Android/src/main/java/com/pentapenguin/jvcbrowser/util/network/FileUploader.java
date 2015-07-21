package com.pentapenguin.jvcbrowser.util.network;

import android.os.AsyncTask;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;

import java.io.*;

public class FileUploader extends AsyncTask<Void, Integer, String> {

        private DefaultHttpClient mHttpClient;
        private HttpParams mParams = new BasicHttpParams();
        private String mFilePath;
        private String mUrl;
        private FileUploaderListener mListener;
        private String mName;

        public FileUploader(FileUploaderListener l, String url) {
                mParams.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                this.mUrl = url;
                mHttpClient = new DefaultHttpClient(mParams);
                mListener = l;
        }

        private String upload() {
                HttpPost httppost = new HttpPost(mUrl);
                File file = new File(mFilePath);
                final long totalBytes = file.length();
                MultipartEntityBuilder multipartEntityBuilder;
                multipartEntityBuilder = MultipartEntityBuilder.create();
                multipartEntityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                multipartEntityBuilder.addPart(mName, new FileBody(file));
                httppost.setEntity(multipartEntityBuilder.build());
                try {
                        return mHttpClient.execute(httppost, new ResponseHandler<String>() {

                                @Override
                                public String handleResponse(HttpResponse response)
                                        throws IOException {
                                        BufferedReader bufferedReader = new BufferedReader(
                                                new InputStreamReader(response.getEntity()
                                                        .getContent()));
                                        StringBuilder stringBuffer = new StringBuilder("");
                                        String line;
                                        String LineSeparator = System.getProperty("line.separator");
                                        while ((line = bufferedReader.readLine()) != null) {
                                                stringBuffer.append(line + LineSeparator);
                                        }
                                        bufferedReader.close();

                                        return stringBuffer.toString();
                                }
                        });
                } catch (ClientProtocolException e) {
                        e.printStackTrace();
                } catch (IOException e) {
                        e.printStackTrace();
                }

                return null;
        }

        @Override
        protected String doInBackground(Void... params) {
                return upload();
        }

        @Override
        protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if(mListener != null) mListener.onComplete(result);

        }

        public void setFileData(String name, String filePath) {
                this.mFilePath = filePath;
                this.mName = name;
        }
}
