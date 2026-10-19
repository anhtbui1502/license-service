import { useCallback, useEffect, useState } from 'react';
import { Alert, App, Button, Card, Descriptions, Input, Popconfirm, Result, Space, Statistic, Typography } from 'antd';
import { DownloadOutlined, MailOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import { Link, useParams } from 'react-router-dom';
import { api, downloadUrl, errorCode, errorMessage, License, LicenseRequest } from '../api';
import { fmtDiff, fmtNum, fmtTime } from '../format';
import StatusTag from '../components/StatusTag';

function useNow(intervalMs = 1000) {
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    const t = setInterval(() => setNow(Date.now()), intervalMs);
    return () => clearInterval(t);
  }, [intervalMs]);
  return now;
}

export default function RequestDetailPage() {
  const { id } = useParams();
  const { message } = App.useApp();
  const [req, setReq] = useState<LicenseRequest>();
  const [license, setLicense] = useState<License>();
  const [otp, setOtp] = useState('');
  const [otpError, setOtpError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const now = useNow();

  const load = useCallback(() => {
    api.get<LicenseRequest>(`/requests/${id}`).then((r) => {
      setReq(r.data);
      if (r.data.licenseId) {
        api.get<License>(`/licenses/${r.data.licenseId}`).then((l) => setLicense(l.data)).catch(() => undefined);
      }
    }).catch((e) => message.error(errorMessage(e)));
  }, [id, message]);

  useEffect(load, [load]);

  if (!req) return null;

  const pending = req.status === 'PENDING_OTP';
  const expired = pending && !!req.otpExpiresAt && dayjs(req.otpExpiresAt).valueOf() <= now;
  const resendWait = req.nextResendAt ? Math.ceil((dayjs(req.nextResendAt).valueOf() - now) / 1000) : 0;
  const prev = req.prevLicense;

  const verify = async () => {
    setBusy(true);
    setOtpError(undefined);
    try {
      const r = await api.post<License>(`/requests/${req.id}/verify`, { otp });
      setLicense(r.data);
      message.success('Đã sinh file license');
      load();
    } catch (e) {
      setOtpError(errorMessage(e));
      setOtp('');
      const code = errorCode(e);
      if (code === 'OTP_INVALID' || code === 'OTP_LOCKED' || code === 'OTP_EXPIRED' || code === 'REQUEST_NOT_PENDING') {
        load();
      }
    } finally {
      setBusy(false);
    }
  };

  const resend = async () => {
    setBusy(true);
    try {
      const r = await api.post<LicenseRequest>(`/requests/${req.id}/resend-otp`);
      setReq(r.data);
      setOtpError(undefined);
      message.success('Đã gửi lại OTP. Mã cũ không còn hiệu lực.');
    } catch (e) {
      message.error(errorMessage(e));
      load();
    } finally {
      setBusy(false);
    }
  };

  const cancel = async () => {
    try {
      const r = await api.post<LicenseRequest>(`/requests/${req.id}/cancel`);
      setReq(r.data);
      message.success('Đã hủy yêu cầu');
    } catch (e) {
      message.error(errorMessage(e));
    }
  };

  return (
    <Space direction="vertical" size="large" style={{ width: '100%', maxWidth: 900 }}>
      <Card title={<>Yêu cầu {req.requestCode} <StatusTag status={req.status} /></>}>
        <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
          <Descriptions.Item label="Thị trường">
            <Link to={`/markets/${req.marketId}`}>{req.marketName} ({req.marketCode})</Link></Descriptions.Item>
          <Descriptions.Item label="Người tạo">{req.requestedBy}</Descriptions.Item>
          <Descriptions.Item label="Max Sub">
            <b>{fmtNum(req.maxSub)}</b> {prev && <Typography.Text type="secondary">
              (v{prev.version}: {fmtNum(prev.maxSub)}, {fmtDiff(prev.maxSub, req.maxSub)})</Typography.Text>}
          </Descriptions.Item>
          <Descriptions.Item label="Max Active Sub">
            <b>{fmtNum(req.maxActiveSub)}</b> {prev && <Typography.Text type="secondary">
              (v{prev.version}: {fmtNum(prev.maxActiveSub)}, {fmtDiff(prev.maxActiveSub, req.maxActiveSub)})</Typography.Text>}
          </Descriptions.Item>
          <Descriptions.Item label="Lý do" span={2}>{req.reason}</Descriptions.Item>
          <Descriptions.Item label="OTP gửi tới">{req.approverEmail}</Descriptions.Item>
          <Descriptions.Item label="Tạo lúc">{fmtTime(req.createdAt)}</Descriptions.Item>
          {req.closedAt && <Descriptions.Item label="Đóng lúc">{fmtTime(req.closedAt)} ({req.closedBy})</Descriptions.Item>}
        </Descriptions>
      </Card>

      {pending && req.canAct && !expired && (
        <Card title="Nhập OTP">
          <Space direction="vertical" size="middle" style={{ width: '100%' }}>
            <Alert type="info" showIcon icon={<MailOutlined />}
                   message={<>OTP đã gửi tới <b>{req.approverEmail}</b>. Liên hệ người duyệt để nhận mã.</>} />
            <Space size="large" wrap>
              <Statistic.Countdown title="OTP hết hạn sau" value={dayjs(req.otpExpiresAt).valueOf()} format="mm:ss"
                                   onFinish={load} />
              <Statistic title="Số lần nhập còn lại" value={req.attemptsLeft} />
              <Statistic title="Số lần gửi lại còn lại" value={req.resendsLeft} />
            </Space>
            <Input.OTP length={6} value={otp} onChange={setOtp} disabled={busy} />
            {otpError && <Alert type="error" showIcon message={otpError} />}
            <Space wrap>
              <Button type="primary" onClick={verify} loading={busy} disabled={otp.length !== 6}>Xác nhận & sinh file</Button>
              <Button onClick={resend} disabled={busy || req.resendsLeft <= 0 || resendWait > 0}>
                {resendWait > 0 ? `Gửi lại OTP (${resendWait}s)` : 'Gửi lại OTP'}
              </Button>
              <Popconfirm title="Hủy yêu cầu này?" onConfirm={cancel} okText="Hủy yêu cầu" cancelText="Không">
                <Button danger disabled={busy}>Hủy yêu cầu</Button>
              </Popconfirm>
            </Space>
          </Space>
        </Card>
      )}

      {pending && expired && (
        <Alert type="warning" showIcon message="OTP đã hết hạn. Yêu cầu sẽ tự đóng, hãy tạo yêu cầu mới."
               action={<Button size="small" onClick={load}>Tải lại</Button>} />
      )}
      {pending && !req.canAct && !expired && (
        <Alert type="info" showIcon message="Yêu cầu đang chờ OTP. Chỉ người tạo hoặc ADMIN được nhập OTP." />
      )}
      {req.status === 'FAILED' && (
        <Alert type="error" showIcon message="Yêu cầu đã bị khóa do nhập sai OTP quá số lần cho phép." />
      )}

      {license && (
        <Card>
          <Result status="success" title={`Đã sinh license ${license.licenseCode}`}
                  subTitle={<>File: {license.fileName}<br />SHA-256: <Typography.Text code copyable>{license.fileSha256}</Typography.Text></>}
                  extra={<Button type="primary" icon={<DownloadOutlined />} href={downloadUrl(license.id)}>Tải file license</Button>} />
        </Card>
      )}
    </Space>
  );
}
