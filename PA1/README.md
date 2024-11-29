# 디자인 문서

## 개요

이 프로젝트는 Java로 구현된 간단한 웹 서버로, 클라이언트의 요청에 따라 적절한 HTML 페이지나 이미지를 제공합니다. 쿠키를 사용하여 사용자의 방문 정보를 저장하고, 재방문 시 이전에 방문한 페이지로 리다이렉트하는 기능을 포함합니다.

## 클래스 및 메서드 설명

### WebServer.java

- `public static void main(String[] args)`
  - 프로그램의 진입점
  - 포트 번호와 쿠키 사용 여부를 설정하고, `ServerSocket`을 생성하여 클라이언트의 연결을 대기
  - 클라이언트로부터 연결이 오면 `handler(client, dest)` 메서드를 통해 요청을 처리

- `private static void handler(Socket client, ArrayList<StructureDest> dest)`
  - 클라이언트의 요청을 처리
  - 요청 메서드가 `GET`인지 확인하고, 요청된 경로에 따라 적절한 핸들러를 호출
  - 쿠키를 확인하여 사용자 ID를 식별하여 응답을 생성

- `private static boolean isDestinationType(String path)`
  - 경로가 `/mountains`, `/beach`, `/city` 중 하나인지 확인하여 해당 경로가 목적지 타입인지 판단

- `private static boolean isDetailType(String path, ArrayList<StructureDest> dest)`
  - 경로가 특정 여행지의 상세 페이지인지 확인
  - `destinations.json`에 있는 리스트에서 여행지 이름과 비교하여 일치 여부를 판단

### helper/JsonFileHandler.java

데이터베이스 대신, 로컬 파일 `data.json`을 사용하여 데이터를 저장

- `public static void writeJson(String filePath, String key, String value)`
  - 주어진 키와 값을 JSON 형식으로 파일에 저장
  - 기존 데이터에 새로운 데이터를 추가한 후 파일에 다시 저장

- `public static String readJson(String filePath)`
  - 지정된 파일 경로에서 JSON 데이터를 읽고 String 형태의 json을 반환
  - 파일이 존재하지 않으면 새로 생성

- `public static Map<String, String> parseJson(String json)`
  - JSON 형식의 문자열을 파싱하여 `Map<String, String>` 형태로 반환

### HTTPHandler 패키지

#### RequestHandler.java

- 인터페이스로서, 모든 요청 핸들러가 구현해야 할 메서드를 정의
- `void handle(Socket client)`
  - 클라이언트의 요청을 처리하는 메서드
- `void setAdditionalInfo(...)`
  - 요청 처리에 필요한 추가 정보를 설정
- `String makeRandomID()`
  - 랜덤한 사용자 ID를 생성하여 필요한 경우 각 핸들러에서 사용

#### IndexHandler.java

- `public class IndexHandler implements RequestHandler`
  - 메인 페이지를 처리하는 핸들러 클래스
  - 브라우저의 쿠키 정보를 확인하여 신규 사용자에게는 쿠키를 설정하고, 기존 사용자에게는 이전에 방문한 페이지로 리다이렉트

- `public void handle(Socket client)`
  - `index.html`을 클라이언트에게 제공
  - 필요에 따라 쿠키를 설정하고, 리다이렉션을 수행

#### DestinationHandler.java

- `public class DestinationHandler implements RequestHandler`
  - 특정 여행지 타입 페이지 (`/mountains`, `/beach`, `/city`)를 처리하는 핸들러 클래스
  - 해당 타입의 여행지 목록을 생성하여 페이지에 표시

- `public void handle(Socket client)`
  - 여행지 타입 페이지를 클라이언트에게 제공
  - 쿠키가 없는 경우 설정하고, 사용자의 선택을 저장

#### DetailHandler.java

- `public class DetailHandler implements RequestHandler`
  - 특정 여행지의 상세 페이지를 처리하는 핸들러 클래스
  - 여행지의 상세 정보(이름, 타입, 설명, 이미지)를 페이지에 표시

- `public void handle(Socket client)`
  - 상세 페이지를 클라이언트에게 제공
  - 쿠키가 없는 경우 설정하고, 사용자의 선택(destination)을 저장

#### NotFoundHandler.java

- `public class NotFoundHandler implements RequestHandler`
  - 요청된 경로가 존재하지 않을 때 404 에러를 반환하는 핸들러 클래스
  - 요청된 리소스가 이미지인 경우 이미지를 반환

- `public void handle(Socket client)`
  - 404 응답 또는 이미지를 클라이언트에게 제공

### structure 패키지

#### StructureDest.java

- `public class StructureDest`
  - 여행지의 정보를 담는 클래스
  - 필드: `name`, `type`, `description`, `image`
  - 생성자 및 Getter 메서드를 제공

#### DestinationArray.java

- `public class DestinationArray`
  - `StructureDest` 객체의 리스트를 담는 클래스
  - Getter 메서드를 제공

#### AdditionalInfo.java

- `public class AdditionalInfo`
  - 요청 처리에 필요한 추가 정보를 저장하는 클래스
  - 필드: `UserID`, `hasCookie`, `det(path)`, `dest`
  - Setter 및 Getter 메서드를 제공
