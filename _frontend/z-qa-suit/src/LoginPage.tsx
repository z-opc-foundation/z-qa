import { useState } from 'react'
import { Button, Card, Form, Input, message } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { menuItems } from './pages-manifest'

/**
 * 登录页（lead 008 §16）—— CTC/SSO 接入前的 mock 门闩。
 *
 * 008/001 凭证规范：口令不进前端源码。mock 口令由构建期环境变量注入，
 * 生产构建不注入即恒不接受任何口令（恒失败），页面也不再把它渲染出来。
 */
const MOCK_AUTH = {
    username: import.meta.env.VITE_MOCK_USER || '',
    password: import.meta.env.VITE_MOCK_PASSWORD || '',
}

interface LoginValues { username: string; password: string }

export default function LoginPage() {
    const [loading, setLoading] = useState(false)
    const mockEnabled = MOCK_AUTH.username !== '' && MOCK_AUTH.password !== ''

    const handleSubmit = async (values: LoginValues) => {
        setLoading(true)
        try {
            if (mockEnabled && values.username === MOCK_AUTH.username && values.password === MOCK_AUTH.password) {
                localStorage.setItem('token', 'mock-' + MOCK_AUTH.username)
                localStorage.setItem('userInfo', JSON.stringify({ name: MOCK_AUTH.username, role: '管理员' }))
                message.success('登录成功（mock 模式，待 CTC/SSO 接入）')
                window.location.href = menuItems[0].key
            } else {
                message.error(mockEnabled ? '用户名或密码错误' : '未配置 mock 登录（VITE_MOCK_USER/VITE_MOCK_PASSWORD），请走 CTC/SSO')
            }
        } finally {
            setLoading(false)
        }
    }

    return (
        <div style={{
            height: '100vh', display: 'flex', justifyContent: 'center', alignItems: 'center',
            background: 'linear-gradient(135deg, #eef2ff 0%, #f5f3ff 50%, #eff6ff 100%)',
        }}>
            <Card style={{ width: 400, boxShadow: '0 10px 40px rgba(124,58,237,0.12)' }}>
                <div style={{ textAlign: 'center', marginBottom: 20 }}>
                    <div style={{
                        width: 48, height: 48, borderRadius: 12, margin: '0 auto 12px',
                        background: 'linear-gradient(135deg, #10b981, #6d28d9)',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        color: '#fff', fontSize: 24, fontWeight: 700,
                    }}>z</div>
                    <div style={{ fontSize: 18, fontWeight: 600, color: '#0f172a' }}>z-qa 测试平台</div>
                </div>
                <Form onFinish={handleSubmit} size="large">
                    <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                        <Input prefix={<UserOutlined />} placeholder="用户名" />
                    </Form.Item>
                    <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 8 }}>
                        <Button type="primary" htmlType="submit" loading={loading} block
                                style={{ background: 'linear-gradient(135deg, #10b981 0%, #6d28d9 100%)', border: 'none' }}>
                            登 录
                        </Button>
                    </Form.Item>
                    {mockEnabled && (
                        <div style={{ textAlign: 'center', color: '#94a3b8', fontSize: 12 }}>
                            Mock 模式已启用（口令由 VITE_MOCK_* 注入，未落源码）
                        </div>
                    )}
                </Form>
            </Card>
        </div>
    )
}
