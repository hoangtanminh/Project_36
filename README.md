# Auction House

Auction House là hệ thống đấu giá trực tuyến theo mô hình client-server. Dự án sử dụng Java, JavaFX, TCP socket, Maven multi-module và một protocol ứng dụng riêng để nhiều client có thể đăng nhập, tạo phiên đấu giá, đặt giá, theo dõi cập nhật realtime và quản lý trạng thái đấu giá.

## Mục tiêu dự án

- Thiết kế và hiện thực một application-layer protocol cho hệ thống đấu giá.
- Xây dựng chương trình client-server có thể phục vụ nhiều client đồng thời.
- Áp dụng TCP socket, object stream, concurrency control và event push từ server đến client.
- Tách hệ thống thành các module rõ ràng: shared model/protocol, server, client.
- Kiểm soát lỗi bằng exception domain rõ nghĩa và phản hồi lỗi có cấu trúc.
- Cung cấp giao diện JavaFX cho các workflow chính của bidder, seller và admin.

## Tính năng chính

- Đăng nhập và đăng ký tài khoản theo vai trò `BIDDER`, `SELLER`, `ADMIN`.
- Xem dashboard phiên đấu giá, trạng thái đấu giá, giá hiện tại và lịch sử bid.
- Seller có thể tạo, cập nhật, bắt đầu, kết thúc, hủy và quản lý phiên đấu giá.
- Bidder có thể nạp tiền, đặt bid, cấu hình auto-bid và thanh toán phiên thắng.
- Admin có thể thực hiện các thao tác quản trị theo quyền.
- Server push event realtime khi đấu giá thay đổi, ví dụ có bid mới hoặc trạng thái auction đổi.
- Dữ liệu auction được lưu bằng SQLite, dữ liệu user được lưu bằng JSON.
- Có sample data để chạy thử ngay sau khi start server.

## Kiến trúc

```text
Project_36
├── auction-shared   Shared domain model, DTO, protocol, enum, exception
├── auction-server   TCP server, service layer, DAO, persistence, event publisher
└── auction-client   JavaFX GUI, controller, session model, socket client service
```

### Module `auction-shared`

Chứa các class dùng chung giữa client và server:

- Domain model: `Auction`, `Bid`, `User`, `Seller`, `Bidder`, `Admin`, `Item`.
- DTO: `AuctionView`, `DashboardView`, `UserView`, `BidView`.
- Protocol object: `ClientRequest`, `ServerResponse`, `LoginRequest`, `BidRequest`, `CreateAuctionRequest`.
- Enum: `CommandType`, `ResponseStatus`, `UserRole`, `AuctionEventType`.
- Exception domain: `AuthenticationException`, `InvalidBidException`, `UnauthorizedActionException`, `ClientConnectionException`, `ClientProtocolException`, `ServerResponseException`.

### Module `auction-server`

Server nhận kết nối TCP, xử lý request từ client và gửi response hoặc event realtime.

Thành phần chính:

- `AuctionServerApplication`: entry point khởi động server.
- `AuctionServer`: mở socket server và accept nhiều client.
- `ClientSession`: xử lý request/response cho từng client.
- `AuthenticationService`: login, register, seed user.
- `AuctionService`: nghiệp vụ đấu giá, bid, auto-bid, payment, authorization.
- `AuctionEventPublisher`: publish event đến các client đang subscribe.
- `SqliteAuctionDao`: lưu auction vào SQLite.
- `FileBackedUserDao`: lưu user vào JSON.

### Module `auction-client`

Client là ứng dụng JavaFX, kết nối đến server qua TCP socket.

Thành phần chính:

- `AuctionClientApp`: entry point JavaFX.
- `AuctionClientService`: gửi `ClientRequest`, nhận `ServerResponse`, xử lý event realtime.
- `AppCoordinator`: điều hướng giữa các màn hình.
- Controllers: login, register, dashboard, auction detail, seller view, account view.
- FXML/CSS: giao diện JavaFX.

## Protocol

Client và server giao tiếp qua TCP socket bằng Java object serialization.

Luồng request-response chính:

```text
Client
  -> ClientRequest<Payload>(CommandType, payload)
Server
  -> ServerResponse<Payload>(ResponseStatus, message, payload, eventType)
```

Server có thể gửi event không gắn trực tiếp với request hiện tại:

```text
ServerResponse(status = EVENT, eventType = BID_PLACED | AUCTION_UPDATED | ...)
```

Client tách riêng:

- `SUCCESS`: đưa vào hàng chờ response của request hiện tại.
- `ERROR`: ném `ServerResponseException`.
- `EVENT`: chuyển đến event listener để cập nhật UI realtime.

## Yêu cầu môi trường

- JDK 21 hoặc mới hơn.
- Maven 3.9 hoặc mới hơn.
- JavaFX được Maven tải qua dependency `org.openjfx`.
- SQLite JDBC được Maven tải qua dependency `org.xerial`.

Kiểm tra nhanh:

```powershell
java -version
mvn -version
```

## Cài đặt

Clone project và cài dependency:

```powershell
git clone <repository-url>
cd Project_36
mvn clean install
```

Nếu chỉ muốn compile nhanh:

```powershell
mvn -q -DskipTests compile
```

## Chạy ứng dụng

Luôn chạy server trước, sau đó mở một hoặc nhiều client.

### Chạy server

Mặc định server dùng port `5050`.

```powershell
mvn -pl auction-server -am -Prun-server process-classes
```

Chỉ định port khác:

```powershell
mvn -pl auction-server -am -Prun-server process-classes -Dauctionhouse.port=5051
```

Server sẽ ghi port đang hoạt động vào:

```text
auction-server/data/active-port.txt
```

Nếu port yêu cầu đang bận, server tự tìm port kế tiếp trong phạm vi cho phép.

### Chạy client

```powershell
mvn -pl auction-client -am -Prun-client process-classes
```

Client mặc định đọc port từ `auction-server/data/active-port.txt`. Có thể chỉ định host và port thủ công:

```powershell
mvn -pl auction-client -am -Prun-client process-classes -Dauctionhouse.host=localhost -Dauctionhouse.port=5050
```

## Tài khoản mẫu

Server tự seed dữ liệu mẫu khi khởi động.

| Username | Password | Vai trò |
| --- | --- | --- |
| `seller01` | `seller01` | Seller |
| `bidder01` | `bidder01` | Bidder |
| `bidder02` | `bidder02` | Bidder |
| `admin01` | `admin01` | Admin |

Một số auction mẫu:

| Auction ID | Sản phẩm | Trạng thái mẫu |
| --- | --- | --- |
| `A1001` | Sony Alpha A7 IV | Running |
| `A1002` | Bose QC Ultra | Open |
| `A1003` | Framework Laptop 16 | Finished |
| `A1004` | Oversized Cotton Hoodie | Paid |
| `A1005` | ASUS ROG Zephyrus G16 | Canceled |

## Cấu hình runtime

Server và client hỗ trợ cấu hình bằng system property hoặc environment variable.

| Cấu hình | Ý nghĩa | Mặc định |
| --- | --- | --- |
| `auctionhouse.port` hoặc `AUCTIONHOUSE_PORT` | Port TCP của server/client | `5050` |
| `auctionhouse.host` hoặc `AUCTIONHOUSE_HOST` | Host server mà client kết nối | `localhost` |
| `auctionhouse.storageDir` | Thư mục lưu dữ liệu server | `auction-server/data` hoặc `data` |
| `auctionhouse.userstore` | File JSON lưu user | `<storageDir>/users.json` |

Ví dụ:

```powershell
mvn -pl auction-server -am -Prun-server process-classes -Dauctionhouse.storageDir=auction-server/data-dev
```

## Chạy test

Chạy toàn bộ test:

```powershell
mvn test
```

Chạy test cho client và các module phụ thuộc:

```powershell
mvn -q -pl auction-client -am clean test
```

Chạy test cho server:

```powershell
mvn -q -pl auction-server -am test
```

Chạy test cho shared model/protocol:

```powershell
mvn -q -pl auction-shared test
```

## Kiểm soát lỗi

Hệ thống dùng exception rõ nghĩa trong `auction-shared` để phân loại lỗi:

- `AuthenticationException`: lỗi đăng nhập, đăng ký, thông tin tài khoản.
- `InvalidAuctionException`: dữ liệu auction không hợp lệ.
- `InvalidBidException`: bid không hợp lệ.
- `AuctionClosedException`: thao tác sai với trạng thái auction.
- `UnauthorizedActionException`: người dùng không đủ quyền.
- `ClientConnectionException`: client không kết nối được hoặc mất kết nối.
- `ClientProtocolException`: client nhận payload không đúng protocol.
- `ServerResponseException`: server trả `ResponseStatus.ERROR`.

Server chuyển lỗi nghiệp vụ thành `ServerResponse.error(...)`; client nhận lỗi này và ném exception tương ứng ở tầng client service.

## Ghi chú phát triển

- `auction-shared` nên giữ ổn định vì đây là contract giữa client và server.
- Khi thêm command mới, cần cập nhật `CommandType`, request DTO, `ClientSession` và `AuctionClientService`.
- Logic nghiệp vụ nên đặt ở `auction-server/service`, không đặt trong JavaFX controller.
- UI controller chỉ nên điều phối input, gọi service và cập nhật view.
- Test client nên tập trung vào `AuctionClientService`; UI JavaFX có thể smoke test hoặc test thủ công nếu không có logic phức tạp.

## Công nghệ sử dụng

- Java 21
- JavaFX 21
- Maven
- JUnit 5
- SQLite JDBC
- Gson
- JaCoCo
