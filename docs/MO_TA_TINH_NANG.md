# Service quản lý license vOCS — Mô tả bài toán và yêu cầu tính năng

> Tài liệu bàn giao cho người tiếp nhận phát triển.
> Phạm vi: mô tả **bài toán nghiệp vụ** và **yêu cầu tính năng**. Không đi vào chi tiết kỹ thuật (công nghệ, thiết kế CSDL, API…). Người tiếp nhận tự quyết các phần đó.

---

## 1. Bối cảnh và bài toán gốc

### 1.1. Hệ thống vOCS và cơ chế license hiện tại

- vOCS được bán cho **nhiều thị trường** (nhiều quốc gia hoặc nhà mạng).
- **Thị trường Việt Nam không quản lý license.**
- Các thị trường còn lại được quản lý license như sau:
  - VHT cấp cho mỗi thị trường **một file license đã mã hóa**.
  - Mỗi thị trường có một service cung cấp Open API của vOCS. Service này **giải mã và đọc file license**, trong đó có 2 tham số giới hạn:

    | Tham số | Ý nghĩa | Thuê bao được tính |
    |---|---|---|
    | **Max Sub** | Tổng số thuê bao đã đấu nối lên hệ thống | Idle, Active, chặn 1 chiều, chặn 2 chiều (**không** tính thuê bao đã hủy) |
    | **Max Active Sub** | Số thuê bao đang hoạt động | Active, chặn 1 chiều, chặn 2 chiều (**không** tính Idle và đã hủy) |

  - Service API **từ chối đấu nối mới hoặc kích hoạt thuê bao** nếu thao tác đó làm vượt một trong hai giới hạn trên.
- Khi một thị trường cần mở rộng thêm thuê bao, VHT dùng **một tool nội bộ** để sinh file license mới với các con số mới rồi gửi cho thị trường.

> Phần cơ chế kiểm tra giới hạn ở phía thị trường chỉ là bối cảnh. Service cần xây dựng **không** thay đổi phần đó.

### 1.2. Vấn đề

1. Tool sinh license **không lưu lịch sử** các file đã sinh cho từng thị trường.
2. Vì vậy **Giám đốc không nắm được** mỗi thị trường đang được cấp bao nhiêu, đã mở rộng bao nhiêu lần, khi nào, do ai thực hiện.
3. Ai có quyền dùng tool là sinh được license với con số tùy ý, **không cần ai phê duyệt**.

### 1.3. Yêu cầu của Giám đốc

> Mỗi khi thị trường yêu cầu sinh thêm license, hệ thống phải **gửi OTP đến email của Giám đốc**. Email ghi **tên thị trường yêu cầu và 2 con số mới**, kèm **mã OTP**. Khi đã nắm được thông tin và đồng ý, Giám đốc cung cấp lại mã OTP cho người thực hiện để sinh file.

### 1.4. Mục tiêu của service

Xây dựng một service có **giao diện web** để:

1. **Quản lý lịch sử** toàn bộ file license đã sinh cho từng thị trường.
2. **Sinh license mới** cho một thị trường, bắt buộc phải qua **bước duyệt bằng OTP** gửi đến email người duyệt (Giám đốc).
3. Cho Giám đốc và các bên liên quan **tra cứu được con số hiện hành và lịch sử** của mọi thị trường.

---

## 2. Từ bài toán đến yêu cầu

Bảng dưới đây giải thích mỗi nhóm yêu cầu ở mục 6 sinh ra từ vấn đề nào, để người làm hiểu **vì sao** cần tính năng đó.

| # | Vấn đề hoặc nhu cầu gốc | Hệ quả thành yêu cầu |
|---|---|---|
| 1 | Không có lịch sử file đã sinh | Mọi file sinh ra phải được **lưu lại vĩnh viễn**, gắn với thị trường, có số phiên bản, người sinh, thời điểm; **tải lại được đúng file cũ** (nhóm F4, F5) |
| 2 | Giám đốc không nắm được con số | Có màn hình xem **license hiện hành và lịch sử của từng thị trường** (nhóm F1, F5) |
| 3 | Sinh license không cần duyệt | Sinh license phải qua **yêu cầu → OTP gửi Giám đốc → nhập OTP → sinh file** (nhóm F2, F3) |
| 4 | Giám đốc phải biết mình đang duyệt cái gì | Email OTP ghi **tên thị trường, 2 con số mới**, kèm số hiện tại và chênh lệch để dễ quyết định (nhóm F3) |
| 5 | Con số được duyệt phải đúng là con số được ghi vào file | **Sau khi gửi OTP thì không sửa được con số**. OTP chỉ dùng được cho đúng yêu cầu đó (quy tắc BR-04, BR-05) |
| 6 | Thị trường Việt Nam không quản lý license | Thị trường có thuộc tính "có quản lý license hay không"; nếu không thì **không cho tạo yêu cầu** (BR-01) |
| 7 | Tránh làm phiền và tránh dò mã OTP | **Giới hạn** số yêu cầu đang chờ, số lần gửi lại, số lần nhập sai, số email mỗi giờ, số lần gọi chức năng (nhóm F7) |
| 8 | Cần biết ai đã làm gì | **Nhật ký thao tác** và **phân quyền** người dùng (nhóm F6, F8) |
| 9 | Chưa có thuật toán mã hóa thật | Phần mã hóa phải **tách riêng, thay thế được** (mục 9 và 10) |

---

## 3. Đối tượng sử dụng

| Vai trò | Là ai | Được làm gì |
|---|---|---|
| **Người duyệt** | Giám đốc (một email được cấu hình sẵn) | Nhận email OTP. Đồng ý thì gửi mã OTP cho người thực hiện, không đồng ý thì không gửi. *Không bắt buộc phải đăng nhập hệ thống.* |
| **Quản trị (Admin)** | Người quản lý hệ thống | Toàn quyền: quản lý thị trường, người dùng, xem nhật ký, và mọi việc của Người thực hiện |
| **Người thực hiện (Operator)** | Nhân viên tiếp nhận yêu cầu từ thị trường | Tạo yêu cầu cấp license, nhập OTP, sinh và tải file |
| **Người xem (Viewer)** | Giám đốc, kế toán, kinh doanh… | Chỉ xem thị trường, yêu cầu và lịch sử license. Không tải file, không thao tác |

---

## 4. Thuật ngữ

| Thuật ngữ | Giải thích |
|---|---|
| **Thị trường** | Một khách hàng hoặc quốc gia dùng vOCS, được cấp file license riêng |
| **License hiện hành** | File license mới nhất đã sinh cho một thị trường, chứa 2 con số đang có hiệu lực |
| **Phiên bản** | Số thứ tự file license của một thị trường (v1, v2, v3…), tự tăng mỗi lần sinh |
| **Yêu cầu cấp license** | Một lần đề nghị sinh license mới cho một thị trường, với 2 con số mới và lý do |
| **OTP** | Mã 6 chữ số gửi qua email cho Người duyệt, dùng **một lần** cho **đúng một yêu cầu** |

---

## 5. Luồng nghiệp vụ chính

```
 Người thực hiện                       Hệ thống                              Người duyệt (Giám đốc)
 ───────────────                       ────────                              ─────────────────────
 1. Nhận đề nghị mở rộng từ thị trường
 2. Chọn thị trường, nhập Max Sub,
    Max Active Sub mới, lý do  ─────▶ 3. Kiểm tra hợp lệ, lưu yêu cầu,
                                          trạng thái "Chờ OTP"
                                       4. Sinh OTP, gửi email ─────────────▶ 5. Đọc email: thị trường,
                                                                                số cũ → số mới, lý do
                                                                             6. Đồng ý → gửi OTP cho
 ◀────────────────────────────────────────────────────────────────────────────  người thực hiện
                                                                                (điện thoại, chat…)
                                                                                Không đồng ý → không gửi
 7. Nhập OTP ──────────────────────▶ 8. OTP đúng → sinh file license,
                                          lưu lịch sử, phiên bản +1
 9. Tải file, gửi cho thị trường ◀──
```

Các nhánh phụ:

- **OTP sai**: báo sai và số lần thử còn lại. Sai quá số lần cho phép thì yêu cầu bị khóa.
- **OTP hết hạn**: yêu cầu tự đóng, phải tạo yêu cầu mới.
- **Không nhận được email**: người thực hiện bấm **gửi lại OTP**, có giới hạn. Mã cũ mất hiệu lực.
- **Nhập nhầm số hoặc thị trường đổi ý**: người thực hiện **hủy yêu cầu** rồi tạo yêu cầu mới.

---

## 6. Yêu cầu tính năng

### F1. Quản lý thị trường

| Mã | Yêu cầu |
|---|---|
| F1.1 | Xem danh sách thị trường gồm: mã, tên, quốc gia, có quản lý license không, license hiện hành (phiên bản, 2 con số, ngày cấp), yêu cầu đang chờ OTP (nếu có), trạng thái hoạt động |
| F1.2 | Admin thêm thị trường: mã (duy nhất, không sửa được sau khi tạo), tên, quốc gia, có quản lý license không, ghi chú |
| F1.3 | Admin sửa thông tin thị trường, bật/tắt quản lý license, bật/tắt hoạt động |
| F1.4 | Xem chi tiết một thị trường: thông tin chung, license hiện hành, yêu cầu đang chờ, **toàn bộ lịch sử file license** của thị trường đó |

### F2. Tạo yêu cầu cấp license mới

| Mã | Yêu cầu |
|---|---|
| F2.1 | Người thực hiện chọn thị trường. Chỉ hiện thị trường **có quản lý license** và **đang hoạt động** |
| F2.2 | Khi chọn thị trường, hiển thị license hiện hành (phiên bản, Max Sub, Max Active Sub) hoặc "Chưa có" |
| F2.3 | Nhập Max Sub mới, Max Active Sub mới và **lý do / số hợp đồng** (bắt buộc) |
| F2.4 | Hiển thị **chênh lệch** so với license hiện hành ngay khi nhập (ví dụ +200.000) |
| F2.5 | Bấm gửi thì hiện **màn hình xác nhận** tóm tắt thị trường, 2 con số và chênh lệch. Nếu số mới **nhỏ hơn** số hiện hành thì cảnh báo rõ (thị trường có thể bị chặn đấu nối hoặc kích hoạt) |
| F2.6 | Xác nhận xong thì hệ thống lưu yêu cầu, gửi OTP đến Người duyệt và chuyển sang màn hình nhập OTP |
| F2.7 | Nếu thị trường đang có yêu cầu chờ OTP, **không cho tạo thêm**. Hiển thị yêu cầu đang chờ (mã, người tạo) để người dùng vào xử lý hoặc hủy |

### F3. OTP và duyệt

| Mã | Yêu cầu |
|---|---|
| F3.1 | Email gửi Người duyệt có: **tên + mã thị trường**, mã yêu cầu, người yêu cầu, lý do, bảng **số hiện tại → số đề nghị → chênh lệch** cho cả 2 tham số, **mã OTP**, thời điểm hết hạn, và dòng nhắc "Nếu không đồng ý, vui lòng KHÔNG cung cấp mã OTP" |
| F3.2 | Tiêu đề email có mã yêu cầu, tên thị trường và 2 con số mới, để Người duyệt phân biệt được các email |
| F3.3 | Màn hình nhập OTP hiển thị: email đã gửi tới, **đồng hồ đếm ngược** thời hạn OTP, số lần nhập còn lại, số lần gửi lại còn lại |
| F3.4 | **Gửi lại OTP**: sinh mã mới, mã cũ mất hiệu lực, thời hạn tính lại. Nút chỉ bấm được sau thời gian chờ và còn lượt gửi lại. Email gửi lại ghi rõ là "gửi lại" |
| F3.5 | **Hủy yêu cầu**: người tạo hoặc Admin hủy được yêu cầu đang chờ |
| F3.6 | Chỉ **người tạo yêu cầu hoặc Admin** được nhập OTP, gửi lại hoặc hủy. Người khác chỉ xem |
| F3.7 | Nhập sai OTP: báo lỗi kèm số lần còn lại. Sai quá số lần cho phép thì yêu cầu chuyển sang "Bị khóa" |
| F3.8 | Hết thời hạn OTP thì yêu cầu tự chuyển sang "Hết hạn", kể cả khi không ai thao tác |
| F3.9 | Nếu gửi email thất bại thì **không tạo yêu cầu** và báo lỗi để người dùng thử lại |

### F4. Sinh file license

| Mã | Yêu cầu |
|---|---|
| F4.1 | OTP đúng thì hệ thống sinh file license với **đúng 2 con số trong yêu cầu** (là 2 con số đã gửi trong email) |
| F4.2 | Tự gán **phiên bản** tiếp theo cho thị trường (v1, v2…) và một **mã license** dễ đọc (ví dụ `MM-V3-20261019`) |
| F4.3 | File mới trở thành **license hiện hành** của thị trường. File cũ vẫn được giữ trong lịch sử |
| F4.4 | Lưu kèm "dấu vân tay" (checksum) của file để sau này đối chiếu một file bất kỳ có đúng do hệ thống sinh ra không |
| F4.5 | Hiển thị kết quả: mã license, tên file, checksum, nút **tải file** |

### F5. Lịch sử và tra cứu

| Mã | Yêu cầu |
|---|---|
| F5.1 | Danh sách **toàn bộ file license** đã sinh, lọc được theo thị trường. Mỗi dòng có: thị trường, phiên bản (đánh dấu bản hiện hành), mã license, 2 con số, yêu cầu gốc, người sinh, thời điểm, checksum |
| F5.2 | **Tải lại đúng file cũ** đã sinh (không sinh lại). Chỉ Admin và Người thực hiện được tải. Mỗi lần tải đều ghi nhật ký |
| F5.3 | Danh sách **yêu cầu cấp license**, lọc theo trạng thái và thị trường, xem được chi tiết từng yêu cầu |

### F6. Người dùng và phân quyền

| Mã | Yêu cầu |
|---|---|
| F6.1 | Đăng nhập bằng tài khoản và mật khẩu. Hết phiên thì phải đăng nhập lại |
| F6.2 | Admin thêm, sửa, khóa người dùng, gán vai trò Admin / Người thực hiện / Người xem, đặt lại mật khẩu |
| F6.3 | Admin không tự khóa hoặc tự hạ quyền chính mình được (tránh mất quyền quản trị) |
| F6.4 | Khi hệ thống chạy lần đầu, có cách tạo tài khoản Admin đầu tiên |

### F7. Chống spam OTP và giới hạn tải

| Mã | Yêu cầu | Giá trị mặc định (cấu hình được) |
|---|---|---|
| F7.1 | Mỗi thị trường chỉ có **tối đa 1 yêu cầu chờ OTP** tại một thời điểm, kể cả khi hai người thao tác cùng lúc | 1 |
| F7.2 | Thời hạn hiệu lực của OTP | 30 phút |
| F7.3 | Thời gian chờ tối thiểu giữa 2 lần gửi lại OTP | 60 giây |
| F7.4 | Số lần gửi lại OTP tối đa cho một yêu cầu | 3 lần |
| F7.5 | Số lần nhập sai OTP tối đa cho một yêu cầu (cộng dồn qua các lần gửi lại) | 5 lần |
| F7.6 | Tổng số email OTP hệ thống được gửi trong 1 giờ | 30 |
| F7.7 | Số email OTP một người dùng được kích hoạt trong 1 giờ | 10 |
| F7.8 | Giới hạn **tần suất gọi** các chức năng nhạy cảm: đăng nhập, tạo yêu cầu, gửi lại OTP, nhập OTP, và giới hạn chung cho toàn hệ thống. Vượt giới hạn thì báo "thử lại sau N giây" | Ví dụ: đăng nhập 10 lần/phút/máy, nhập OTP 10 lần/phút/người |

### F8. Nhật ký hệ thống

| Mã | Yêu cầu |
|---|---|
| F8.1 | Ghi lại: đăng nhập (thành công / thất bại), tạo yêu cầu, gửi OTP, nhập sai OTP, khóa yêu cầu, hủy yêu cầu, sinh file, tải file, thay đổi thị trường, thay đổi người dùng |
| F8.2 | Mỗi bản ghi có: thời điểm, người thực hiện, hành động, đối tượng, chi tiết, địa chỉ máy |
| F8.3 | Admin xem và lọc nhật ký theo hành động và người dùng. **Không ai được sửa hoặc xóa nhật ký** |

---

## 7. Quy tắc nghiệp vụ

| Mã | Quy tắc |
|---|---|
| BR-01 | Thị trường **không quản lý license** (Việt Nam) hoặc **ngừng hoạt động** thì không tạo được yêu cầu |
| BR-02 | Max Sub và Max Active Sub là số nguyên dương, **Max Active Sub ≤ Max Sub** |
| BR-03 | Không cho tạo yêu cầu có **cả 2 con số giống hệt** license hiện hành |
| BR-04 | Sau khi gửi OTP thì **không sửa được** thị trường và 2 con số của yêu cầu. Muốn đổi thì hủy và tạo mới |
| BR-05 | Mỗi OTP chỉ dùng được **một lần**, cho **đúng một yêu cầu**. Hệ thống **không lưu mã OTP ở dạng đọc được** |
| BR-06 | Mỗi thị trường chỉ có tối đa **1 yêu cầu chờ OTP** (xem lý do ở mục 11) |
| BR-07 | Yêu cầu đã kết thúc (đã sinh file, hết hạn, hủy, bị khóa) thì **không mở lại được** |
| BR-08 | File license và lịch sử **không bị xóa**. Mỗi thị trường có đúng **một license hiện hành** |
| BR-09 | Email Người duyệt là **cấu hình của hệ thống**, người tạo yêu cầu không chọn hay sửa được |

---

## 8. Trạng thái của yêu cầu cấp license

```
                      ┌──(nhập đúng OTP)──────────▶ ĐÃ SINH FILE
                      │
   [Tạo yêu cầu] ──▶ CHỜ OTP ──(quá thời hạn)──────▶ HẾT HẠN
                      │
                      ├──(người tạo/Admin hủy)────▶ ĐÃ HỦY
                      │
                      └──(sai OTP quá số lần)─────▶ BỊ KHÓA
```

| Trạng thái | Ý nghĩa | Chiếm chỗ "đang chờ" của thị trường? |
|---|---|---|
| Chờ OTP | Đã gửi OTP, chờ nhập | **Có** |
| Đã sinh file | Hoàn tất, đã có file license | Không |
| Hết hạn | Quá thời hạn OTP mà chưa nhập đúng | Không |
| Đã hủy | Người tạo hoặc Admin hủy | Không |
| Bị khóa | Nhập sai OTP quá số lần | Không |

---

## 9. Các màn hình cần có

| Màn hình | Nội dung chính | Ai dùng |
|---|---|---|
| Đăng nhập | Tài khoản, mật khẩu | Tất cả |
| Danh sách thị trường | Bảng thị trường + license hiện hành + yêu cầu đang chờ; nút "Tạo license" ở từng dòng | Tất cả (thêm/sửa: Admin) |
| Chi tiết thị trường | Thông tin, license hiện hành, cảnh báo nếu đang có yêu cầu chờ, lịch sử file | Tất cả |
| Tạo license mới | Chọn thị trường → số hiện hành → nhập số mới (hiện chênh lệch) → lý do → xác nhận → gửi OTP | Admin, Người thực hiện |
| Chi tiết yêu cầu / nhập OTP | Thông tin yêu cầu, đếm ngược, ô nhập 6 số, gửi lại, hủy; sau khi thành công thì hiện kết quả và nút tải file | Tất cả xem; thao tác: người tạo, Admin |
| Danh sách yêu cầu | Lọc theo trạng thái, thị trường | Tất cả |
| Lịch sử file license | Lọc theo thị trường, tải file | Tất cả xem; tải: Admin, Người thực hiện |
| Người dùng | Thêm, sửa, khóa, đặt lại mật khẩu | Admin |
| Nhật ký hệ thống | Lọc theo hành động, người dùng | Admin |

---

## 10. Yêu cầu chung (phi chức năng, ở mức nghiệp vụ)

- Hệ thống chỉ dùng **trong mạng nội bộ VHT** (hoặc qua VPN).
- **Khóa hoặc thuật toán mã hóa license phải được bảo vệ**: không để trong mã nguồn hay ở chỗ người dùng xem được.
- Phần mã hóa file license phải **tách riêng, thay thế được**. Hiện **chưa có thuật toán thật**, giai đoạn đầu dùng định dạng thử nghiệm.
- Giao diện tiếng Việt; số hiển thị có dấu phân cách hàng nghìn (500.000); thời gian theo giờ Việt Nam.
- Dữ liệu lịch sử license và nhật ký phải được **sao lưu định kỳ**, vì đây là "sổ cái" của license.

---

## 11. Các quyết định đã chốt và lý do

| Quyết định | Lý do |
|---|---|
| **Mỗi thị trường chỉ 1 yêu cầu chờ OTP** (đã cân nhắc phương án 3 và không chọn) | License là trạng thái nối tiếp (v3 → v4). Nhiều yêu cầu cùng chờ thì sẽ dựa trên số cũ và lỗi thời ngay khi một yêu cầu được duyệt. Giám đốc dễ nhầm giữa nhiều email cùng thị trường. Thực tế mỗi thị trường chỉ mở rộng vài lần mỗi năm. Muốn sửa số thì **hủy rồi tạo lại**. Giới hạn này vẫn để dạng cấu hình để sau này đổi được nếu cần |
| Giám đốc **không cần đăng nhập** | Đúng yêu cầu gốc: chỉ nhận email và chuyển OTP cho người thực hiện |
| Gửi lại OTP **không tạo yêu cầu mới** | Giữ một mã yêu cầu duy nhất, tránh làm đầy hộp thư Giám đốc |
| Làm **đơn giản trước**, các tính năng bổ sung để sau | Ưu tiên có ngay lịch sử và cơ chế duyệt. Bắt buộc phải có chống spam OTP và giới hạn tần suất |

---

## 12. Ngoài phạm vi giai đoạn này (để sau)

- Import các file license **đã cấp trước đây** vào lịch sử (đọc file cũ để lấy 2 con số).
- Biểu đồ lịch sử Max Sub / Max Active Sub theo thời gian.
- Nút **Duyệt / Từ chối** ngay trong email (thay cho việc chuyển OTP thủ công).
- Nút **"Thay thế yêu cầu"** (tự hủy yêu cầu cũ và tạo yêu cầu mới trong một bước).
- Người duyệt dự phòng, **duyệt nhiều cấp** khi tăng vượt một ngưỡng.
- Lấy **mức sử dụng thực tế** từ thị trường để cảnh báo khi sắp chạm giới hạn.
- Ghi thêm thông tin nhận diện vào file license (mã thị trường, phiên bản…) để thị trường không dùng nhầm hay quay lại file cũ. Việc này cần phối hợp với service phía thị trường.
- Đăng nhập bằng tài khoản công ty (LDAP / SSO).

---

## 13. Vấn đề còn mở, cần làm rõ với người giao việc

1. **Thuật toán mã hóa và định dạng file license**: chưa được cung cấp. Cần lấy từ tool sinh license hiện tại hoặc từ đội phụ trách service Open API ở thị trường.
2. **Tool sinh license cũ** có bị thu hồi hay khóa lại không? Nếu tool cũ vẫn dùng được thì có thể sinh license mà không qua OTP, và lịch sử vẫn bị thiếu.
3. **Email Người duyệt chính thức** và có cần người duyệt dự phòng (khi Giám đốc vắng) không.
4. Các giá trị giới hạn ở mục F7 (thời hạn OTP, số lần gửi lại…) đã phù hợp chưa.
5. Có cho phép **giảm** con số license không, hay chỉ cảnh báo như hiện tại.

---

## 14. Hiện trạng

Đã có **bản làm thử (MVP)** trong repository này, nhánh `claude/compassionate-brahmagupta-aq2dd6`, bao phủ các nhóm F1–F8. Phần mã hóa đang ở dạng thử nghiệm. Người tiếp nhận có thể tham khảo, phát triển tiếp hoặc làm lại theo công nghệ của mình. Tài liệu này mới là căn cứ về yêu cầu.
