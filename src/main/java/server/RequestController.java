package server;

import database.MemberDAO;
import database.MemberVO;
import http.HttpRequest;
import http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

public class RequestController {

    private static final MemberDAO memberDAO = new MemberDAO();

    public static Map<String, String> sessionContext = new HashMap<>();
    public static Map<String, String> headers = new HashMap<String, String>() {
        {
            put("Server", "boardcar-server");
        }
    };

    public static Function<HttpRequest, HttpResponse> httpTest = request -> HttpResponse.ok(headers, "httpTest Success");
    public static Function<HttpRequest, HttpResponse> login = request -> {

        // HTTP request body에서 JSON parse
        String id, password;
        try {
            JSONObject jsonObject = new JSONObject(request.getBody());
            id = jsonObject.getString("id");
            password = jsonObject.getString("password");
        } catch (JSONException e) {
            // JSON이 잘못 되었을 때
            e.printStackTrace();
            return HttpResponse.badRequest(headers, "Invalid body (JSON format)");
        }

        // DB에서 회원 ID 찾기
        try {
            MemberVO requestMember = memberDAO.getMemberById(id);

            // PW가 틀린 경우
            if (!requestMember.getPassword().equals(password)) {
                return HttpResponse.badRequest(headers, "login failed (mismatch PASSWORD)");
            }
        } catch (SQLException e) {
            // ID를 찾지 못했을 때
            e.printStackTrace();
            return HttpResponse.badRequest(headers, "login failed (not found ID)");
        }

        // 로그인 성공! -> 세션 생성
        UUID uuid = UUID.randomUUID();
        sessionContext.put(uuid.toString(), id);

        // 헤더에 추가
        headers.put("Session-Key", uuid.toString());

        return HttpResponse.ok(headers, "login success");
    };

    public static Function<HttpRequest, HttpResponse> myInfo = request -> {

        // 세션 체크
        String targetId;
        if ((targetId = getIdFromSessionContext(request)) == null) {
            return HttpResponse.badRequest(headers, "please login before access DB");
        }

        // DB에서 Member 레코드 가져와서 JSON 형식으로 body에 저장
        try {
            // SQL 실행
            MemberVO targetMember = memberDAO.getMemberById(targetId);
            String body = targetMember.toString();

            return HttpResponse.ok(headers, body);

        } catch (SQLException e) {
            return HttpResponse.badRequest(headers, e.toString());
        }

    };

    public static Function<HttpRequest, HttpResponse> members = request -> {

        // 세션 체크
        if (getIdFromSessionContext(request) == null) {
            return HttpResponse.badRequest(headers, "please login before access DB");
        }

        // DB에서 member 정보 가져오기
        try {
            // SQL 실행
            List<MemberVO> memberVOList  = memberDAO.getMemberVOList();

            // member 정보를 가진 JSON body 만들기 -> split 구분자 \n
            StringBuilder stringBuilder = new StringBuilder();
            for(MemberVO memberVO : memberVOList){
                stringBuilder.append(memberVO.toString()).append("\n");
            }

            return HttpResponse.ok(headers, stringBuilder.toString());

        } catch (SQLException e) {
            return HttpResponse.badRequest(headers, e.toString());
        }
    };

    public static Function<HttpRequest, HttpResponse> changePassword = request -> {

        // 세션 체크
        String targetId;
        if ((targetId = getIdFromSessionContext(request)) == null) {
            return HttpResponse.badRequest(headers, "please login before access DB");
        }


        // 비밀번호 변경
        try {
            // body json에서 비밀번호 추출
            JSONObject jsonObject = new JSONObject(request.getBody());
            String newPassword = jsonObject.getString("password");

            // SQL 실행
            memberDAO.updateMemberPassword(targetId, newPassword);
            return HttpResponse.ok(headers, "Password is changed successfully");
        } catch (SQLException e) {
            return HttpResponse.badRequest(headers, e.toString());
        }
    };


    public static Function<HttpRequest, HttpResponse> other = request -> HttpResponse.notFound(headers, "Wrong API access");

    public static String getIdFromSessionContext(HttpRequest request) {
        String sessionKey = request.getHeaders().getOrDefault("Session-Key", null);

        return sessionContext.getOrDefault(sessionKey, null);
    }
}
