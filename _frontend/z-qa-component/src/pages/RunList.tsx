import React, {useEffect, useMemo, useState} from 'react';
import {Button, Card, Input, Progress, Select, Space, Table, Tag, Tooltip} from 'antd';
import {EmptyState} from '@yuku123/z-frontend-common/ui';
import {EyeOutlined, FileSearchOutlined, ReloadOutlined} from '@ant-design/icons';
import {runApi} from '../services/api';
import {useNavigate} from 'react-router-dom';
import {useQaBase} from '../qaBase';

/**
 * 执行记录列表 + 实时状态轮询.
 * 路径: /z-qa/runs
 */
const RunList: React.FC = () => {
    const [runs, setRuns] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const [keyword, setKeyword] = useState('');
    const [status, setStatus] = useState<string | undefined>();
    const navigate = useNavigate();
    const qaRoot = useQaBase();

    const load = async () => {
        setLoading(true);
        try {
            const res = await runApi.list({limit: 100});
            setRuns(res?.content || []);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
        const timer = setInterval(load, 5000);  // 5s 轮询
        return () => clearInterval(timer);
    }, []);

    const filtered = useMemo(() => {
        const kw = keyword.trim().toLowerCase();
        return runs.filter((r) => {
            if (status && r.status !== status) return false;
            if (!kw) return true;
            return [r.run_code, r.name, r.trigger_user].some(
                (v) => v && String(v).toLowerCase().includes(kw));
        });
    }, [runs, keyword, status]);

    const statusColors: Record<string, string> = {
        success: 'success', failed: 'error', running: 'processing', pending: 'default', cancelled: 'warning',
    };

    return (
        <div style={{padding: 0}}>
            <Card
                title={<Space><FileSearchOutlined/> 执行记录</Space>}
                extra={
                    <Space>
                        <Input.Search
                            allowClear
                            placeholder="搜 run_code / 套件 / 触发人"
                            style={{width: 240}}
                            onChange={(e) => setKeyword(e.target.value)}
                        />
                        <Select
                            allowClear
                            placeholder="状态"
                            style={{width: 130}}
                            value={status}
                            onChange={setStatus}
                            options={['pending', 'running', 'success', 'failed', 'cancelled'].map((v) => ({value: v, label: v}))}
                        />
                        <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
                    </Space>
                }
            >
                <Table
                    rowKey="id"
                    dataSource={filtered}
                    loading={loading}
                    pagination={{
                        pageSize: 20,
                        showSizeChanger: true,
                        pageSizeOptions: ['10', '20', '50', '100'],
                        showTotal: (t) => `共 ${t} 条`,
                    }}
                    locale={{
                        emptyText: runs.length === 0
                            ? <EmptyState title="还没有执行记录" description="去「测试套件」页触发一次运行"/>
                            : <EmptyState title="没有匹配的执行记录" description="换个关键词或清掉状态筛选"/>
                    }}
                    columns={[
                        {
                            title: 'Run Code', dataIndex: 'run_code', width: 220,
                            render: t => <code style={{fontSize: 11}}>{t}</code>
                        },
                        {title: '套件', dataIndex: 'name', ellipsis: true},
                        {
                            title: '触发方式', dataIndex: 'trigger_type', width: 100,
                            render: t => <Tag>{t}</Tag>
                        },
                        {
                            title: '状态', dataIndex: 'status', width: 100,
                            render: s => <Tag color={statusColors[s]}>{s}</Tag>
                        },
                        {
                            title: '进度', width: 200,
                            render: (_, r: any) => {
                                const total = Number(r.total_steps) || 0;
                                const done = (Number(r.passed_steps) || 0) + (Number(r.failed_steps) || 0);
                                const percent = total > 0 ? Math.round((done / total) * 100) : 0;
                                return (
                                    <Tooltip title={`${done}/${total}`}>
                                        <Progress percent={percent} size="small"
                                                  status={r.status === 'failed' ? 'exception' :
                                                      r.status === 'success' ? 'success' :
                                                          r.status === 'running' ? 'active' : 'normal'}/>
                                    </Tooltip>
                                );
                            }
                        },
                        {
                            title: '通过/失败', width: 110,
                            render: (_, r: any) => (
                                <Space>
                                    <Tag color="success">{r.passed_steps || 0}</Tag>
                                    <Tag color={r.failed_steps > 0 ? 'error' : 'default'}>{r.failed_steps || 0}</Tag>
                                </Space>
                            )
                        },
                        {
                            title: '耗时', dataIndex: 'duration_ms', width: 90,
                            render: t => t ? `${t}ms` : '-'
                        },
                        {title: '触发人', dataIndex: 'trigger_user', width: 100},
                        {
                            title: '操作', width: 100, fixed: 'right',
                            render: (_, r) => (
                                <Button type="link" size="small" icon={<EyeOutlined/>}
                                        onClick={() => navigate(`${qaRoot}/runs/${r.id}`)}>详情</Button>
                            )
                        },
                    ]}
                />
            </Card>
        </div>
    );
};

export default RunList;