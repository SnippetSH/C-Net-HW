# ProxyServer 디자인 문서

## 개요
`ProxyServer`는 클라이언트의 요청을 받아 웹 서버에 전달하고, 그 응답을 클라이언트에게 반환하는 프록시 서버입니다. 이 서버는 개인화되지 않은 페이지에 대해 해당 정보를 캐싱하며, 쿠키를 통해 사용자 식별을 지원합니다.

폴더 구조:  
```
├─resources  
└─src  
    ├─ProxyServer
    │   └─Main.java
    └─WebServer  
        ├─helper  
        ├─HTTPHandler  
        └─structure  
        └─Main.java
```
ProxyServer 실행 방법:  
```bash
java -jar ProxyServer.jar
```

WebServer 실행 방법:  
```bash
java -jar WebServer.jar
```

## 프로그램 흐름
- 브라우저에서 localhost:8085/ 또는 localhost:8085/index.html 로 접속.  
- ProxyServer는 클라이언트의 요청을 파싱해서 / 경로에 대한 캐싱이 존재하는지 확인
- 캐싱이 존재하면 WebServer에 If-Modified-Since 헤더를 포함한 요청을 보냄
- WebServer는 해당 요청에 대해 수정되었는지 확인하고 응답을 전달
  - 수정되었으면 200 OK 응답과 함께 응답 데이터를 ProxyServer에 전달
  - 수정되지 않았으면 304 Not Modified만을 담은 헤더를 ProxyServer에 전달
- ProxyServer는 캐싱이 존재하지 않으면 WebServer에 직접 요청을 보냄


위의 과정에서는 index.html 파일에 대해서만 다루며,  
나머지 경로에 대한 처리는 개인화된 응답이 포함되어 있는 것으로 간주하여 ProxyServer에서 곧바로 WebServer로 요청을 전달


또한, WebServer에서 헤더를 파싱할 때 If-Modified-Since 헤더와 Client의 Cookie가 동시에 있고,  
개인화된 페이지를 제공할 수 있는 경우 index.html 파일에 대한 304 Not Modified 응답이 아닌 302 Found 응답을 보냄

--> 이전에 방문했던 유저의 index 요청은 302 Found을 통해 개인화된 페이지로 리다이렉트

## 주요 기능
1. **서버 시작 및 클라이언트 요청 수신**:
   - 서버는 지정된 포트(8085)에서 클라이언트의 연결을 대기
   - 클라이언트가 연결되면 요청을 처리

2. **클라이언트 요청 처리**:
   - 클라이언트의 요청이 `GET` 메소드로 시작하는지 확인
   - 요청 경로를 추출하고, 허용된 경로 목록과 비교하여 접근을 허용할지 결정
   - 이미지 파일 요청(`.jpg`)에 대해서는 웹 서버에 직접 요청을 전달하고, 응답을 클라이언트에게 반환

3. **캐싱 및 조건부 요청**:
   - 캐시된 응답이 있는 경우, `If-Modified-Since` 헤더를 사용하여 웹 서버에 조건부 요청을 보냄
   - 웹 서버의 응답이 `304 Not Modified`인 경우, 캐시된 데이터를 클라이언트에게 반환
   - 새로운 응답이 `200 OK`이고, `Cache-Control` 헤더에 `max-age`가 있는 경우, 응답 데이터를 캐시에 저장하고 클라이언트에게 반환

4. **쿠키 처리**:
   - 클라이언트의 요청 헤더에 쿠키가 존재할 경우 쿠키를 추출하여 웹 서버에 전달
   - 쿠키를 통해 개인화된 페이지를 제공.

## 코드 설명
- **Main 클래스**: 프록시 서버의 진입점으로, 서버 소켓을 열고 클라이언트의 요청을 처리
- **handleClientRequest 메소드**: 클라이언트의 요청을 읽고, 웹 서버에 요청을 전달하며, 응답을 클라이언트에게 반환
- **CachedResponse 클래스**: 캐시된 응답 데이터를 저장하는 클래스입니다. 응답 데이터와 마지막 수정 시간을 포함

## 코드 구조
- `main` 메소드: 서버 소켓을 열고 클라이언트의 연결을 대기
- `handleClientRequest` 메소드: 클라이언트의 요청을 처리하고, 웹 서버와의 통신을 관리
- `formatDateToHttp` 및 `parseHttpDate` 메소드: HTTP 날짜 형식을 처리하는 유틸리티 메소드

