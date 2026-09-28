/**
 * 步骤状态与缺陷分类的前端口径 —— 与后端 QaStepStatus / QaFailureClassifier 一一对应。
 *
 * 失败态是 fail_ 前缀的一族，别只认 fail：新增一种失败态时只改这里。
 */
export const STEP_STATUS_LABEL: Record<string, string> = {
    success: '通过',
    skip: '跳过',
    pending: '待执行',
    running: '执行中',
    fail_assert: '断言失败',
    fail_system_error: '系统错误',
    fail_timeout: '超时',
    fail_navigation: '无法访问',
    fail_param_null: '变量缺失',
    fail_unknown: '未知失败',
};

const TAG_COLOR: Record<string, string> = {
    success: 'success',
    skip: 'default',
    running: 'processing',
    pending: 'default',
    fail_assert: 'error',
    fail_system_error: 'volcano',
    fail_timeout: 'orange',
    fail_navigation: 'orange',
    fail_param_null: 'gold',
    fail_unknown: 'error',
};

export const DEFECT_LABEL: Record<string, string> = {
    system_defect: '系统缺陷',
    script_defect: '脚本缺陷',
    env_data_defect: '环境/数据缺陷',
};

const DEFECT_TAG_COLOR: Record<string, string> = {
    system_defect: 'volcano',
    script_defect: 'orange',
    env_data_defect: 'gold',
};

export const isFailStatus = (result?: string): boolean => !!result && result.startsWith('fail_');

export const stepStatusLabel = (result?: string): string =>
    !result ? '-' : (STEP_STATUS_LABEL[result] || result);

export const stepStatusTagColor = (result?: string): string =>
    (result && TAG_COLOR[result]) || (isFailStatus(result) ? 'error' : 'default');

export const defectLabel = (defectType?: string): string =>
    !defectType ? '-' : (DEFECT_LABEL[defectType] || defectType);

export const defectTagColor = (defectType?: string): string =>
    (defectType && DEFECT_TAG_COLOR[defectType]) || 'default';
