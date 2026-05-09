import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { getProjectPullRequestReview } from '../services/api';
import SplitPaneComparison from '../components/comparison/SplitPaneComparison';
import './PullRequestReviewPage.css';

function extractTextFromAdfNode(node) {
  if (!node) return '';
  if (typeof node === 'string') return node;
  if (Array.isArray(node)) return node.map(extractTextFromAdfNode).join('');
  if (typeof node !== 'object') return '';
  let text = '';
  if (typeof node.text === 'string') text += node.text;
  if (Array.isArray(node.content)) {
    const inner = node.content.map(extractTextFromAdfNode).join('');
    if (node.type === 'paragraph' || node.type === 'heading') {
      text += `${inner}\n`;
    } else if (node.type === 'listItem') {
      text += `- ${inner}\n`;
    } else {
      text += inner;
    }
  }
  return text;
}

function toReadableIssueDescription(raw) {
  if (raw == null) return '—';
  const text = String(raw).trim();
  if (!text) return '—';
  if (!(text.startsWith('{') || text.startsWith('['))) return text;
  try {
    const parsed = JSON.parse(text);
    const extracted = extractTextFromAdfNode(parsed).replace(/\n{3,}/g, '\n\n').trim();
    return extracted || text;
  } catch {
    return text;
  }
}

function ScoreBadge({ score }) {
  const value = typeof score === 'number' ? Math.round(score * 100) : null;
  return <span className="prr-score-badge">{value != null ? `${value}%` : '—'}</span>;
}

function AiWidget({ widget }) {
  if (!widget) return null;
  return (
    <section className="prr-widget">
      <div className="prr-widget-head">
        <h3>{widget.title}</h3>
        <ScoreBadge score={widget.accuracyScore} />
      </div>
      {widget.summary && <p className="prr-widget-summary">{widget.summary}</p>}
      {(widget.discrepancies || []).length > 0 && (
        <>
          <strong className="prr-widget-label">Findings</strong>
          <ul>
            {(widget.discrepancies || []).map((d, i) => <li key={`${d}-${i}`}>{d}</li>)}
          </ul>
        </>
      )}
      {(widget.evidence || []).length > 0 && (
        <>
          <strong className="prr-widget-label">Evidence</strong>
          <ul>
            {(widget.evidence || []).map((e, i) => <li key={`${e}-${i}`}>{e}</li>)}
          </ul>
        </>
      )}
    </section>
  );
}

function buildFeedbackFromWidget(widget, severity) {
  if (!widget) return [];
  const title = widget.title || 'AI Feedback';
  const discrepancyItems = (widget.discrepancies || []).map((item, idx) => ({
    id: `${title}-d-${idx}`,
    severity,
    title,
    message: item,
  }));
  const evidenceItems = (widget.evidence || []).map((item, idx) => ({
    id: `${title}-e-${idx}`,
    severity: 'info',
    title: `${title} (Evidence)`,
    message: item,
  }));
  return [...discrepancyItems, ...evidenceItems];
}

export default function PullRequestReviewPage() {
  const { projectId } = useParams();
  const [search] = useSearchParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const issueKey = search.get('issueKey') || '';
  const prNumber = search.get('prNumber') || '';

  useEffect(() => {
    if (!projectId || (!issueKey && !prNumber)) {
      setError('PR review için issueKey veya prNumber gerekli.');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    getProjectPullRequestReview(projectId, {
      issueKey: issueKey || undefined,
      prNumber: prNumber ? Number(prNumber) : undefined,
    })
      .then((res) => setData(res?.data ?? res))
      .catch((e) => setError(e.message || 'PR review yüklenemedi.'))
      .finally(() => setLoading(false));
  }, [projectId, issueKey, prNumber]);

  const issue = useMemo(() => data?.issue || null, [data]);
  const readableIssueDescription = useMemo(
    () => toReadableIssueDescription(issue?.description),
    [issue?.description],
  );
  const comparisonRequirement = useMemo(() => ({
    key: issue?.key || '—',
    summary: issue?.title || 'Issue',
    description: readableIssueDescription,
    assignee: issue?.assignee || '—',
    status: data?.prNumber ? `PR #${data.prNumber}` : undefined,
    priority: issue?.storyPoints != null ? `Story points: ${issue.storyPoints}` : undefined,
    acceptanceCriteria: [
      ...((data?.reviewProcessAi?.discrepancies || []).map((item) => `Review process: ${item}`)),
      ...((data?.implementationAi?.discrepancies || []).map((item) => `Implementation: ${item}`)),
    ],
  }), [issue?.key, issue?.title, issue?.assignee, issue?.storyPoints, readableIssueDescription, data?.prNumber, data?.reviewProcessAi?.discrepancies, data?.implementationAi?.discrepancies]);
  const comparisonFeedback = useMemo(() => ([
    ...buildFeedbackFromWidget(data?.reviewProcessAi, 'warning'),
    ...buildFeedbackFromWidget(data?.implementationAi, 'error'),
  ]), [data?.reviewProcessAi, data?.implementationAi]);

  return (
    <div className="prr-page">
      <div className="prr-topbar">
        <button type="button" className="spp-btn-load" onClick={() => navigate(-1)}>Back</button>
        {data?.prUrl && (
          <a className="spp-btn-load" href={data.prUrl} target="_blank" rel="noreferrer">
            Open PR on GitHub
          </a>
        )}
      </div>

      {loading && <div className="prr-loading">Loading PR review...</div>}
      {error && <div className="prr-error">{error}</div>}

      {!loading && !error && data && (
        <div className="prr-layout">
          <div className="prr-diff-head">
            <h2>{data.prTitle || `PR #${data.prNumber}`}</h2>
            <span>{data.baseBranch || '-'} ← {data.headBranch || '-'}</span>
          </div>
          <div className="prr-splitpane-wrap">
            <SplitPaneComparison
              requirement={comparisonRequirement}
              diff={data.diff || ''}
              feedback={comparisonFeedback}
              highlightedLines={[]}
              loading={false}
              error={null}
              fileName={`${data.headBranch || 'pull-request'}.diff`}
            />
          </div>
          <div className="prr-ai-widgets">
            <AiWidget widget={data.reviewProcessAi} />
            <AiWidget widget={data.implementationAi} />
          </div>
        </div>
      )}
    </div>
  );
}
