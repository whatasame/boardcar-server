import http.HttpRequest;
import http.HttpResponse;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class HttpClientTestApp {

    public static final String TEST_ID = "testid";
    public static final String TEST_PASSWORD = "testpw";
    public static final String TEST_NEW_PASSWORD = "testpw2";

    public static String sessionKey = null;

    public static void main(String[] args) throws IOException {

        TestMethod.gethttpTest();
        TestMethod.postLogin();
        TestMethod.getMembers();
        TestMethod.getMyInfo();
//        TestMethod.postChangeMyPassword(TEST_NEW_PASSWORD);
        TestMethod.putUploadPost();

    }

    public static HttpResponse sendHttpRequest(HttpRequest httpRequest) throws IOException {

        // 서버 정보 가져오기
        Properties properties = new Properties();
        properties.load(new FileInputStream(".properties"));
//        String SERVER_IP = properties.getProperty("SERVER_IP");
        String SERVER_IP = "localhost";
        final int SERVER_PORT = Integer.parseInt(properties.getProperty("SERVER_PORT"));

        // HTTP 통신
        try {
            // 클라이언트 소켓 열기
            Socket socket = new Socket(SERVER_IP, SERVER_PORT); // 서버 연결

            // 서버에 Request 보내기
            socket.getOutputStream().write(httpRequest.toString().getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            // 서버로부터 Response 받기
            HttpResponse httpResponse = responseBuilder(socket.getInputStream());

            // 클라이언트 소켓 닫기
            socket.close();
            return httpResponse;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static HttpResponse responseBuilder(InputStream inputStream) throws IOException {

        // HTTP 요청 전체 읽기
        StringBuilder stringBuilder = new StringBuilder();
        String inputLine;
        while (!(inputLine = myReadLine(inputStream)).equals("")) {
            stringBuilder.append(inputLine).append("\n"); // sb : HTTP 요청 전체
        }

        // HTTP 요청 한 줄씩 처리
        String response = stringBuilder.toString();
        String[] responseArr = response.split("\n");

        // 헤더 - 요청 부분 parse
        String[] statusLine = responseArr[0].split(" ");
        String version = statusLine[0];
        String statusCode = statusLine[1];
        String statusText = statusLine[2];

        // 남은 헤더 parse
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < responseArr.length; ++i) {
            if (responseArr[i].equals("")) { // 아무것도 없는 줄을 만난다 -> 헤더의 끝을 만남
                break;
            }

            // 헤더의 (키:값)을 해시맵에 저장
            String[] temp = responseArr[i].split(":");
            headers.put(temp[0].trim(), temp[1].trim());
        }

        // 바디 parse
        String body = null;
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        if (contentLength > 0) {

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            for (int i = 0; i < contentLength; i++) {
                byteArrayOutputStream.write(inputStream.read());
            }
            body = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
        }

        return new HttpResponse(version, statusCode, statusText, headers, body);
    }

    private static String myReadLine(InputStream inputStream) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        while (true) {
            int b = inputStream.read();

            if (b == '\n' || b == '\0') {
                break;
            }

            tmp.write(b);
        }
        return new String(tmp.toByteArray(), StandardCharsets.UTF_8).trim();
    }

}

class TestMethod {

    private static String version = "HTTP/1.1";
    private static Map<String, String> headers = new HashMap<String, String>() {{
        put("Content-Type", "text/html;charset=utf-8");
    }};

    public static void gethttpTest() throws IOException {
        /* GET TEST */
        HttpRequest testRequest = new HttpRequest("GET", "/httpTest", version, headers, "");


        HttpResponse testResponse = HttpClientTestApp.sendHttpRequest(testRequest);
        System.out.println("Response Status : " + testResponse.getStatusCode());
        System.out.println(testResponse.getBody());
        System.out.println();
    }

    public static void postLogin() throws IOException {
        /* POST TEST */

        // 로그인 정보 JSON 생성
        JSONObject loginJson = new JSONObject();
        loginJson.put("id", HttpClientTestApp.TEST_ID);
        loginJson.put("password", HttpClientTestApp.TEST_PASSWORD);

        // 요청
        HttpRequest loginRequest = new HttpRequest("POST", "/login", version, headers, loginJson.toString());

        // 응답
        HttpResponse loginResponse = HttpClientTestApp.sendHttpRequest(loginRequest);
        if (loginResponse.getStatusCode().equals("200")) {
            HttpClientTestApp.sessionKey = loginResponse.getHeaders().get("Session-Key");
        }

        System.out.println("Response Status : " + loginResponse.getStatusCode());
        System.out.println(loginResponse.getBody());
        System.out.println("Session-Key is " + HttpClientTestApp.sessionKey);
        System.out.println();
    }

    public static void getMembers() throws IOException {
        /* GET TEST - 멤버 테이블 전체 가져오기 */

        HttpRequest memberListRequest = new HttpRequest("GET", "/members", version, headers, "");
        memberListRequest.putHeader("Session-Key", HttpClientTestApp.sessionKey);

        HttpResponse memberListResponse = HttpClientTestApp.sendHttpRequest(memberListRequest);

        System.out.println("Response Status : " + memberListResponse.getStatusCode());

        String[] memberList = memberListResponse.getBody().split("\n");
        for(String member : memberList){
            System.out.println(member);
        }
        System.out.println();

    }

    public static void getMyInfo() throws IOException {
        /* GET TEST - 자신의 정보 가져오기 */

        HttpRequest infoRequest = new HttpRequest("GET", "/myInfo", version, headers, "");
        infoRequest.putHeader("Session-Key", HttpClientTestApp.sessionKey);

        HttpResponse infoResponse = HttpClientTestApp.sendHttpRequest(infoRequest);

        System.out.println("Response Status : " + infoResponse.getStatusCode());
        System.out.println(infoResponse.getBody());

        System.out.println();
    }

    public static void postChangeMyPassword(String newPassword) throws IOException {
        /* POST TEST - 자신의 비밀번호 변경 */

        // body JSON에서 변경할 비밀번호 parse
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("password", newPassword);

        // 요청
        HttpRequest changePasswordRequest = new HttpRequest("PATCH", "/changePassword", version, headers, jsonObject.toString());

        // 응답
        HttpResponse changePasswordResponse = HttpClientTestApp.sendHttpRequest(changePasswordRequest);
        System.out.println("Response Status : " + changePasswordResponse.getStatusCode());
        System.out.println(changePasswordResponse.getBody());

        System.out.println();

    }

    public static void putUploadPost() throws IOException {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("MID", "testid");
        jsonObject.put("PDATE", "2022-11-11");
        jsonObject.put("TITLE", "테스트 제목");
        jsonObject.put("BODY", "테스트 바디");
        jsonObject.put("TYPE", "자유");

        HttpRequest uploadPostRequest = new HttpRequest("PUT", "/uploadPost", version, headers, jsonObject.toString());

        // 응답
        HttpResponse uploadPostResponse = HttpClientTestApp.sendHttpRequest(uploadPostRequest);
        System.out.println("Response Status : " + uploadPostResponse.getStatusCode());
        System.out.println(uploadPostResponse.getBody());

        System.out.println();

    }
}

