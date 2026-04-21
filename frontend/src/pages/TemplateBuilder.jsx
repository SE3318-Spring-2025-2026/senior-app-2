import { useState } from 'react';
import { createProjectTemplate } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { Navigate } from 'react-router-dom';
import AppDatePicker from '../components/AppDatePicker';
import AppDateRangePicker from '../components/AppDateRangePicker';
import './TemplateBuilder.css';

const GRADING_TYPE_OPTIONS = ['SOFT', 'BINARY'];
const DELIVERABLE_BASE_TYPES = ['STATEMENT_OF_WORK', 'DEMO', 'PROPOSAL'];

function normalizeText(value) {
  return (value || '').trim().toLowerCase();
}

function nextIndexedLabel(existingValues, prefix) {
  const used = new Set(existingValues.map((value) => normalizeText(value)));
  let index = 1;
  let candidate = `${prefix} ${index}`;
  while (used.has(normalizeText(candidate))) {
    index += 1;
    candidate = `${prefix} ${index}`;
  }
  return candidate;
}

function createDefaultRubric(title = 'New Rubric 1') {
  return {
    title,
    criteriaType: 'SOFT',
  };
}

function createDefaultDeliverable(title = 'New Deliverable 1') {
  return {
    type: DELIVERABLE_BASE_TYPES[0],
    title,
    description: '',
    weight: 15,
    autoAddToAllSprints: false,
    autoSyncKey: null,
    fileUploadDeliverable: false,
    submission: {
      allowedContentTypes: ['MARKDOWN'],
      maxSubmissionCount: 1,
      requiresCommitteeAssignment: false,
    },
    rubrics: [createDefaultRubric('New Rubric 1')],
    evaluations: [],
  };
}

function createDefaultSprintEvaluation(title = 'Sprint Evaluation 1') {
  return {
    title,
    weight: 100,
    autoAddToAllSprints: false,
    autoSyncKey: null,
    rubrics: [createDefaultRubric('New Rubric 1')],
  };
}

function createDefaultSprint(nextNo) {
  return {
    sprintNo: nextNo,
    title: `Sprint ${nextNo}`,
    startDate: '',
    endDate: '',
    deliverables: [],
    evaluations: [],
  };
}

function deepClone(value) {
  return structuredClone(value);
}

function parseIsoDate(value) {
  if (!value) return null;
  const [year, month, day] = value.split('-').map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function formatIsoDate(date) {
  if (!date) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function addDays(date, days) {
  const next = new Date(date);
  next.setDate(next.getDate() + days);
  return next;
}

function collectAutoAddItems(sprints, key) {
  const seen = new Set();
  const items = [];
  for (const sprint of sprints) {
    for (const item of sprint[key] || []) {
      if (!item?.autoAddToAllSprints) continue;
      const signature = JSON.stringify({
        type: item.type,
        title: item.title,
        description: item.description,
        weight: item.weight,
        fileUploadDeliverable: item.fileUploadDeliverable,
        rubrics: item.rubrics,
        submission: item.submission,
      });
      if (seen.has(signature)) continue;
      seen.add(signature);
      const clone = deepClone(item);
      if (!clone.autoSyncKey) clone.autoSyncKey = `auto-${key}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      items.push(clone);
    }
  }
  return items;
}

function stripUiOnlyFields(template) {
  const clean = deepClone(template);
  clean.sprints = (clean.sprints || []).map((sprint) => ({
    ...sprint,
    deliverables: (sprint.deliverables || []).map(({ autoSyncKey, ...deliverable }) => deliverable),
    evaluations: (sprint.evaluations || []).map(({ autoSyncKey, type, ...evaluation }) => evaluation),
  }));
  return clean;
}

function TemplateBuilder() {
  const { user, loading } = useAuth();

  const [template, setTemplate] = useState({
    name: 'Senior Project 2026 Template',
    description: 'Default template for senior project workflow',
    term: 'Spring 2026',
    projectStartDate: '',
    sprints: [],
  });
  const [selected, setSelected] = useState({ type: 'template' });
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState({ type: '', message: '' });
  const [clipboard, setClipboard] = useState({
    deliverable: null,
    evaluation: null,
    rubrics: null,
  });
  const [typeQuery, setTypeQuery] = useState('');
  const [showTypeMenu, setShowTypeMenu] = useState(false);

  if (loading) return <div className="loading">Loading...</div>;
  if (!user) return <Navigate to="/" replace />;
  if (user.role !== 'COORDINATOR') return <Navigate to="/access-denied" replace />;

  const updateTemplateField = (field, value) => {
    setTemplate((prev) => ({ ...prev, [field]: value }));
  };

  const addSprint = () => {
    setTemplate((prev) => {
      const nextNo = prev.sprints.length + 1;
      const newSprint = createDefaultSprint(nextNo);
      const previousSprint = prev.sprints[prev.sprints.length - 1];
      const prevStart = parseIsoDate(previousSprint?.startDate);
      const prevEnd = parseIsoDate(previousSprint?.endDate);
      if (prevStart && prevEnd && prevEnd.getTime() >= prevStart.getTime()) {
        const prevDurationDays = Math.floor((prevEnd.getTime() - prevStart.getTime()) / (1000 * 60 * 60 * 24)) + 1;
        const nextStart = addDays(prevEnd, 1);
        const nextEnd = addDays(nextStart, prevDurationDays - 1);
        newSprint.startDate = formatIsoDate(nextStart);
        newSprint.endDate = formatIsoDate(nextEnd);
      }
      newSprint.deliverables = collectAutoAddItems(prev.sprints, 'deliverables');
      newSprint.evaluations = collectAutoAddItems(prev.sprints, 'evaluations');
      const next = [...prev.sprints, newSprint];
      return { ...prev, sprints: next };
    });
    setSelected({ type: 'sprint', sprintIndex: template.sprints.length });
  };

  const removeSprint = (sprintIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      next.sprints.splice(sprintIndex, 1);
      next.sprints = next.sprints.map((s, i) => ({ ...s, sprintNo: i + 1 }));
      return next;
    });
    setSelected({ type: 'template' });
  };

  const addDeliverable = (sprintIndex) => {
    const sprint = template.sprints[sprintIndex];
    const currentTotal = (sprint?.deliverables ?? []).reduce(
      (sum, d) => sum + (Number(d.weight) || 0),
      0
    );
    if (currentTotal >= 100) {
      setStatus({ type: 'error', message: `Sprint ${sprint.sprintNo} deliverable weight is already 100%.` });
      return;
    }
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const existingTitles = next.sprints[sprintIndex].deliverables.map((item) => item.title);
      const nextTitle = nextIndexedLabel(existingTitles, 'New Deliverable');
      next.sprints[sprintIndex].deliverables.push(createDefaultDeliverable(nextTitle));
      return next;
    });
    const deliverableIndex = template.sprints[sprintIndex]?.deliverables?.length ?? 0;
    setSelected({ type: 'deliverable', sprintIndex, deliverableIndex });
  };

  const removeDeliverable = (sprintIndex, deliverableIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      next.sprints[sprintIndex].deliverables.splice(deliverableIndex, 1);
      return next;
    });
    setSelected({ type: 'sprint', sprintIndex });
  };

  const addSprintEvaluation = (sprintIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const existingTitles = next.sprints[sprintIndex].evaluations.map((item) => item.title);
      const nextTitle = nextIndexedLabel(existingTitles, 'Sprint Evaluation');
      next.sprints[sprintIndex].evaluations.push(createDefaultSprintEvaluation(nextTitle));
      return next;
    });
    const evaluationIndex = template.sprints[sprintIndex]?.evaluations?.length ?? 0;
    setSelected({ type: 'sprintEvaluation', sprintIndex, evaluationIndex });
  };

  const removeSprintEvaluation = (sprintIndex, evaluationIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      next.sprints[sprintIndex].evaluations.splice(evaluationIndex, 1);
      return next;
    });
    setSelected({ type: 'sprint', sprintIndex });
  };

  const addDeliverableRubric = (sprintIndex, deliverableIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const targetDeliverable = next.sprints[sprintIndex].deliverables[deliverableIndex];
      const existingTitles = targetDeliverable.rubrics.map((item) => item.title);
      const nextTitle = nextIndexedLabel(existingTitles, 'New Rubric');
      targetDeliverable.rubrics.push(createDefaultRubric(nextTitle));
      if (targetDeliverable.autoAddToAllSprints && targetDeliverable.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].deliverables.length; j += 1) {
            if (i === sprintIndex && j === deliverableIndex) continue;
            const candidate = next.sprints[i].deliverables[j];
            if (candidate.autoSyncKey === targetDeliverable.autoSyncKey) {
              candidate.rubrics = deepClone(targetDeliverable.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const addSprintEvalRubric = (sprintIndex, evaluationIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const targetEvaluation = next.sprints[sprintIndex].evaluations[evaluationIndex];
      const existingTitles = targetEvaluation.rubrics.map((item) => item.title);
      const nextTitle = nextIndexedLabel(existingTitles, 'New Rubric');
      targetEvaluation.rubrics.push(createDefaultRubric(nextTitle));
      if (targetEvaluation.autoAddToAllSprints && targetEvaluation.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].evaluations.length; j += 1) {
            if (i === sprintIndex && j === evaluationIndex) continue;
            const candidate = next.sprints[i].evaluations[j];
            if (candidate.autoSyncKey === targetEvaluation.autoSyncKey) {
              candidate.rubrics = deepClone(targetEvaluation.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const pasteDeliverable = (sprintIndex) => {
    if (!clipboard.deliverable) return;
    setTemplate((prev) => {
      const next = deepClone(prev);
      next.sprints[sprintIndex].deliverables.push(deepClone(clipboard.deliverable));
      return next;
    });
    const deliverableIndex = template.sprints[sprintIndex]?.deliverables?.length ?? 0;
    setSelected({ type: 'deliverable', sprintIndex, deliverableIndex });
  };

  const pasteEvaluation = (sprintIndex) => {
    if (!clipboard.evaluation) return;
    setTemplate((prev) => {
      const next = deepClone(prev);
      next.sprints[sprintIndex].evaluations.push(deepClone(clipboard.evaluation));
      return next;
    });
    const evaluationIndex = template.sprints[sprintIndex]?.evaluations?.length ?? 0;
    setSelected({ type: 'sprintEvaluation', sprintIndex, evaluationIndex });
  };

  const pasteDeliverableRubrics = (sprintIndex, deliverableIndex) => {
    if (!clipboard.rubrics) return;
    setTemplate((prev) => {
      const next = deepClone(prev);
      const targetDeliverable = next.sprints[sprintIndex].deliverables[deliverableIndex];
      targetDeliverable.rubrics = deepClone(clipboard.rubrics);
      if (targetDeliverable.autoAddToAllSprints && targetDeliverable.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].deliverables.length; j += 1) {
            if (i === sprintIndex && j === deliverableIndex) continue;
            const candidate = next.sprints[i].deliverables[j];
            if (candidate.autoSyncKey === targetDeliverable.autoSyncKey) {
              candidate.rubrics = deepClone(targetDeliverable.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const pasteEvaluationRubrics = (sprintIndex, evaluationIndex) => {
    if (!clipboard.rubrics) return;
    setTemplate((prev) => {
      const next = deepClone(prev);
      const targetEvaluation = next.sprints[sprintIndex].evaluations[evaluationIndex];
      targetEvaluation.rubrics = deepClone(clipboard.rubrics);
      if (targetEvaluation.autoAddToAllSprints && targetEvaluation.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].evaluations.length; j += 1) {
            if (i === sprintIndex && j === evaluationIndex) continue;
            const candidate = next.sprints[i].evaluations[j];
            if (candidate.autoSyncKey === targetEvaluation.autoSyncKey) {
              candidate.rubrics = deepClone(targetEvaluation.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const updateDeliverableRubricField = (sprintIndex, deliverableIndex, rubricIndex, field, value) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const targetDeliverable = next.sprints[sprintIndex].deliverables[deliverableIndex];
      targetDeliverable.rubrics[rubricIndex][field] = value;
      if (targetDeliverable.autoAddToAllSprints && targetDeliverable.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].deliverables.length; j += 1) {
            if (i === sprintIndex && j === deliverableIndex) continue;
            const candidate = next.sprints[i].deliverables[j];
            if (candidate.autoSyncKey === targetDeliverable.autoSyncKey) {
              candidate.rubrics = deepClone(targetDeliverable.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const updateSprintEvaluationRubricField = (sprintIndex, evaluationIndex, rubricIndex, field, value) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const targetEvaluation = next.sprints[sprintIndex].evaluations[evaluationIndex];
      targetEvaluation.rubrics[rubricIndex][field] = value;
      if (targetEvaluation.autoAddToAllSprints && targetEvaluation.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].evaluations.length; j += 1) {
            if (i === sprintIndex && j === evaluationIndex) continue;
            const candidate = next.sprints[i].evaluations[j];
            if (candidate.autoSyncKey === targetEvaluation.autoSyncKey) {
              candidate.rubrics = deepClone(targetEvaluation.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const removeDeliverableRubric = (sprintIndex, deliverableIndex, rubricIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const targetDeliverable = next.sprints[sprintIndex].deliverables[deliverableIndex];
      targetDeliverable.rubrics.splice(rubricIndex, 1);
      if (targetDeliverable.autoAddToAllSprints && targetDeliverable.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].deliverables.length; j += 1) {
            if (i === sprintIndex && j === deliverableIndex) continue;
            const candidate = next.sprints[i].deliverables[j];
            if (candidate.autoSyncKey === targetDeliverable.autoSyncKey) {
              candidate.rubrics = deepClone(targetDeliverable.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const removeSprintEvaluationRubric = (sprintIndex, evaluationIndex, rubricIndex) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      const targetEvaluation = next.sprints[sprintIndex].evaluations[evaluationIndex];
      targetEvaluation.rubrics.splice(rubricIndex, 1);
      if (targetEvaluation.autoAddToAllSprints && targetEvaluation.autoSyncKey) {
        for (let i = 0; i < next.sprints.length; i += 1) {
          for (let j = 0; j < next.sprints[i].evaluations.length; j += 1) {
            if (i === sprintIndex && j === evaluationIndex) continue;
            const candidate = next.sprints[i].evaluations[j];
            if (candidate.autoSyncKey === targetEvaluation.autoSyncKey) {
              candidate.rubrics = deepClone(targetEvaluation.rubrics);
            }
          }
        }
      }
      return next;
    });
  };

  const updateSelectedNode = (field, value) => {
    setTemplate((prev) => {
      const next = structuredClone(prev);
      if (selected.type === 'sprint') {
        next.sprints[selected.sprintIndex][field] = value;
      } else if (selected.type === 'deliverable') {
        const sprint = next.sprints[selected.sprintIndex];
        const deliverable = sprint.deliverables[selected.deliverableIndex];
        if (field === 'weight') {
          const deliverables = sprint.deliverables;
          const currentWeight = Number(deliverables[selected.deliverableIndex].weight) || 0;
          const othersTotal = deliverables.reduce(
            (sum, d, i) => sum + (i === selected.deliverableIndex ? 0 : Number(d.weight) || 0),
            0
          );
          const requested = Number(value) || 0;
          const bounded = Math.max(0, Math.min(requested, Math.max(0, 100 - othersTotal)));
          deliverables[selected.deliverableIndex][field] = bounded;
        } else if (field === 'autoAddToAllSprints') {
          if (value) {
            const key = deliverable.autoSyncKey || `auto-deliverables-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
            deliverable.autoAddToAllSprints = true;
            deliverable.autoSyncKey = key;
          } else {
            const key = deliverable.autoSyncKey;
            if (key) {
              for (const sprintItem of next.sprints) {
                sprintItem.deliverables.forEach((item) => {
                  if (item.autoSyncKey === key) {
                    item.autoAddToAllSprints = false;
                    item.autoSyncKey = null;
                  }
                });
              }
            } else {
              deliverable.autoAddToAllSprints = false;
            }
          }
        } else {
          deliverable[field] = value;
        }

        if (field !== 'autoAddToAllSprints' && deliverable.autoAddToAllSprints && deliverable.autoSyncKey) {
          for (let i = 0; i < next.sprints.length; i += 1) {
            for (let j = 0; j < next.sprints[i].deliverables.length; j += 1) {
              if (i === selected.sprintIndex && j === selected.deliverableIndex) continue;
              const item = next.sprints[i].deliverables[j];
              if (item.autoSyncKey === deliverable.autoSyncKey) {
                item[field] = deepClone(deliverable[field]);
              }
            }
          }
        }
      } else if (selected.type === 'sprintEvaluation') {
        const sprint = next.sprints[selected.sprintIndex];
        const evaluation = sprint.evaluations[selected.evaluationIndex];

        if (field === 'autoAddToAllSprints') {
          if (value) {
            const key = evaluation.autoSyncKey || `auto-evaluations-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
            evaluation.autoAddToAllSprints = true;
            evaluation.autoSyncKey = key;
          } else {
            const key = evaluation.autoSyncKey;
            if (key) {
              for (const sprintItem of next.sprints) {
                sprintItem.evaluations.forEach((item) => {
                  if (item.autoSyncKey === key) {
                    item.autoAddToAllSprints = false;
                    item.autoSyncKey = null;
                  }
                });
              }
            } else {
              evaluation.autoAddToAllSprints = false;
            }
          }
        } else {
          evaluation[field] = value;
        }

        if (field !== 'autoAddToAllSprints' && evaluation.autoAddToAllSprints && evaluation.autoSyncKey) {
          for (let i = 0; i < next.sprints.length; i += 1) {
            for (let j = 0; j < next.sprints[i].evaluations.length; j += 1) {
              if (i === selected.sprintIndex && j === selected.evaluationIndex) continue;
              const item = next.sprints[i].evaluations[j];
              if (item.autoSyncKey === evaluation.autoSyncKey) {
                item[field] = deepClone(evaluation[field]);
              }
            }
          }
        }
      }
      return next;
    });
  };

  const validateBeforeSave = () => {
    if (!template.name.trim() || !template.description.trim() || !template.term.trim()) {
      return 'Template name, description and term are required.';
    }
    if (!template.projectStartDate) {
      return 'Project start date is required.';
    }
    if (!template.sprints.length) return 'At least one sprint is required.';
    if (projectTotalWeight !== 100) {
      return `Project total deliverable weight must be exactly 100% (current: ${projectTotalWeight}%).`;
    }
    for (const sprint of template.sprints) {
      if (!Array.isArray(sprint.deliverables)) return 'Each sprint must include deliverables.';
      if (!Array.isArray(sprint.evaluations) || sprint.evaluations.length === 0) {
        return 'Each sprint must include at least one evaluation.';
      }
      const evaluationTotal = sprint.evaluations.reduce(
        (sum, e) => sum + (Number(e.weight) || 0),
        0
      );
      if (evaluationTotal !== 100) {
        return `Sprint ${sprint.sprintNo} evaluation weights must total exactly 100% (current: ${evaluationTotal}%).`;
      }
      const deliverableNames = new Set();
      for (const deliverable of sprint.deliverables) {
        const name = normalizeText(deliverable.title);
        if (!name) return `Sprint ${sprint.sprintNo} has a deliverable with empty title.`;
        if (deliverableNames.has(name)) {
          return `Sprint ${sprint.sprintNo} cannot have duplicate deliverable titles.`;
        }
        deliverableNames.add(name);

        const rubricNames = new Set();
        for (const rubric of deliverable.rubrics || []) {
          const rubricName = normalizeText(rubric.title);
          if (!rubricName) return `Sprint ${sprint.sprintNo} deliverable rubric title cannot be empty.`;
          if (rubricNames.has(rubricName)) {
            return `Sprint ${sprint.sprintNo} deliverable "${deliverable.title}" has duplicate rubric titles.`;
          }
          rubricNames.add(rubricName);
        }
      }

      const evaluationNames = new Set();
      for (const evaluation of sprint.evaluations) {
        const name = normalizeText(evaluation.title);
        if (!name) return `Sprint ${sprint.sprintNo} has an evaluation with empty title.`;
        if (evaluationNames.has(name)) {
          return `Sprint ${sprint.sprintNo} cannot have duplicate evaluation titles.`;
        }
        evaluationNames.add(name);

        const rubricNames = new Set();
        for (const rubric of evaluation.rubrics || []) {
          const rubricName = normalizeText(rubric.title);
          if (!rubricName) return `Sprint ${sprint.sprintNo} evaluation rubric title cannot be empty.`;
          if (rubricNames.has(rubricName)) {
            return `Sprint ${sprint.sprintNo} evaluation "${evaluation.title}" has duplicate rubric titles.`;
          }
          rubricNames.add(rubricName);
        }
      }

      const deliverableTotal = sprint.deliverables.reduce(
        (sum, d) => sum + (Number(d.weight) || 0),
        0
      );
    }
    return '';
  };

  const saveTemplate = async () => {
    const validationError = validateBeforeSave();
    if (validationError) {
      setStatus({ type: 'error', message: validationError });
      return;
    }
    setSaving(true);
    setStatus({ type: '', message: '' });
    try {
      const res = await createProjectTemplate(stripUiOnlyFields(template));
      setStatus({
        type: 'success',
        message: `Template saved successfully (ID: ${res?.data?.templateId ?? 'n/a'})`,
      });
    } catch (err) {
      setStatus({ type: 'error', message: err.message || 'Failed to save template.' });
    } finally {
      setSaving(false);
    }
  };

  const selectedNode = (() => {
    if (selected.type === 'sprint') return template.sprints[selected.sprintIndex];
    if (selected.type === 'deliverable') {
      return template.sprints[selected.sprintIndex]?.deliverables?.[selected.deliverableIndex];
    }
    if (selected.type === 'sprintEvaluation') {
      return template.sprints[selected.sprintIndex]?.evaluations?.[selected.evaluationIndex];
    }
    return template;
  })();
  const customDeliverableTypes = Array.from(new Set(
    template.sprints
      .flatMap((sprint) => sprint.deliverables || [])
      .map((deliverable) => (deliverable.type || '').trim())
      .filter((type) => type && !DELIVERABLE_BASE_TYPES.includes(type))
  ));
  const allDeliverableTypeOptions = [...DELIVERABLE_BASE_TYPES, ...customDeliverableTypes];
  const projectTotalWeight = template.sprints.reduce(
    (sum, sprint) => sum + (sprint.deliverables || []).reduce((s, d) => s + (Number(d.weight) || 0), 0),
    0
  );

  return (
    <div className="template-builder-page">
      <div className="template-builder-header">
        <h1>Coordinator Template Builder</h1>
        <button className="template-btn" onClick={saveTemplate} disabled={saving}>
          {saving ? 'Saving...' : 'Save Template'}
        </button>
      </div>

      {status.message && (
        <div className={`template-status ${status.type === 'error' ? 'is-error' : 'is-success'}`}>
          {status.message}
        </div>
      )}

      <div className="template-builder-grid">
        <aside className="template-tree">
          <div className="template-tree-header">
            <strong>Template Tree</strong>
          </div>
          <button
            className={`template-node ${selected.type === 'template' ? 'active' : ''}`}
            onClick={() => setSelected({ type: 'template' })}
          >
            Template Root (Project Total: {projectTotalWeight}%)
          </button>
          {template.sprints.map((sprint, sprintIndex) => (
            <div className="template-tree-group" key={`sprint-${sprintIndex}`}>
              <div className="node-row">
                <button
                  className={`template-node ${selected.type === 'sprint' && selected.sprintIndex === sprintIndex ? 'active' : ''}`}
                  onClick={() => setSelected({ type: 'sprint', sprintIndex })}
                >
                  Sprint {sprint.sprintNo}: {sprint.title || 'Untitled'} (D: {(sprint.deliverables || []).reduce((sum, d) => sum + (Number(d.weight) || 0), 0)}% / E: {(sprint.evaluations || []).reduce((sum, e) => sum + (Number(e.weight) || 0), 0)}%)
                </button>
                <button className="delete-node-btn" onClick={() => removeSprint(sprintIndex)} title="Delete sprint">✕</button>
              </div>
              {sprint.evaluations.map((evaluation, evaluationIndex) => (
                <div key={`ev-${sprintIndex}-${evaluationIndex}`} className="template-subtree">
                  <div className="node-row">
                    <button
                      className={`template-node child ${selected.type === 'sprintEvaluation' && selected.sprintIndex === sprintIndex && selected.evaluationIndex === evaluationIndex ? 'active' : ''}`}
                      onClick={() => setSelected({ type: 'sprintEvaluation', sprintIndex, evaluationIndex })}
                    >
                      Evaluation: {evaluation.title || 'Untitled'} ({Number(evaluation.weight) || 0}%)
                    </button>
                    <button
                      className="delete-node-btn"
                      onClick={() => removeSprintEvaluation(sprintIndex, evaluationIndex)}
                      title="Delete evaluation"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
              <div className="template-subtree">
                <div className="template-inline-actions">
                  <button className="template-btn add-uniform" onClick={() => addSprintEvaluation(sprintIndex)}>
                    + Evaluation
                  </button>
                  <button
                    className="template-btn add-uniform"
                    onClick={() => pasteEvaluation(sprintIndex)}
                    disabled={!clipboard.evaluation}
                  >
                    Paste Evaluation
                  </button>
                </div>
              </div>

              {sprint.deliverables.map((deliverable, deliverableIndex) => (
                <div key={`del-${sprintIndex}-${deliverableIndex}`} className="template-subtree">
                  <div className="node-row">
                    <button
                      className={`template-node child ${selected.type === 'deliverable' && selected.sprintIndex === sprintIndex && selected.deliverableIndex === deliverableIndex ? 'active' : ''}`}
                      onClick={() => setSelected({ type: 'deliverable', sprintIndex, deliverableIndex })}
                    >
                      Deliverable: {deliverable.title || 'Untitled'} ({Number(deliverable.weight) || 0}%)
                    </button>
                    <button
                      className="delete-node-btn"
                      onClick={() => removeDeliverable(sprintIndex, deliverableIndex)}
                      title="Delete deliverable"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              ))}
              <div className="template-subtree">
                <div className="template-inline-actions">
                  <button className="template-btn add-uniform" onClick={() => addDeliverable(sprintIndex)}>
                    + Deliverable
                  </button>
                  <button
                    className="template-btn add-uniform"
                    onClick={() => pasteDeliverable(sprintIndex)}
                    disabled={!clipboard.deliverable}
                  >
                    Paste Deliverable
                  </button>
                </div>
              </div>
            </div>
          ))}
          <button className="template-btn add-sprint-bottom" onClick={addSprint}>+ Add Sprint</button>
        </aside>

        <section className="template-editor">
          <h2>Editor</h2>
          {selected.type === 'template' && (
            <div className="template-form">
              <label>Name<input value={template.name} onChange={(e) => updateTemplateField('name', e.target.value)} /></label>
              <label className="field-full">Description<textarea value={template.description} onChange={(e) => updateTemplateField('description', e.target.value)} /></label>
              <label>Term<input value={template.term} onChange={(e) => updateTemplateField('term', e.target.value)} /></label>
              <AppDatePicker
                label="Project Start Date"
                value={template.projectStartDate}
                onChange={(value) => updateTemplateField('projectStartDate', value)}
              />
            </div>
          )}

          {selected.type === 'sprint' && selectedNode && (
            <div className="template-form">
              <label>Sprint No<input type="number" value={selectedNode.sprintNo ?? ''} onChange={(e) => updateSelectedNode('sprintNo', Number(e.target.value))} /></label>
              <label>Title<input value={selectedNode.title ?? ''} onChange={(e) => updateSelectedNode('title', e.target.value)} /></label>
              <AppDateRangePicker
                className="field-full"
                label="Sprint Date Range"
                startDate={selectedNode.startDate ?? ''}
                endDate={selectedNode.endDate ?? ''}
                onChange={(start, end) => {
                  updateSelectedNode('startDate', start);
                  updateSelectedNode('endDate', end);
                }}
              />
            </div>
          )}

          {selected.type === 'deliverable' && selectedNode && (
            <div className="template-form">
              <div className="field-full editor-toolbar">
                <button
                  className="template-btn"
                  type="button"
                  onClick={() => setClipboard((prev) => ({ ...prev, deliverable: deepClone(selectedNode) }))}
                >
                  Copy Deliverable
                </button>
                <button
                  className="template-btn"
                  type="button"
                  onClick={() => pasteDeliverable(selected.sprintIndex)}
                  disabled={!clipboard.deliverable}
                >
                  Paste Deliverable
                </button>
              </div>
              <label>
                Type
                <div className="type-combobox">
                  <input
                    value={selectedNode.type ?? ''}
                    onFocus={() => {
                      setTypeQuery(selectedNode.type ?? '');
                      setShowTypeMenu(true);
                    }}
                    onBlur={() => {
                      window.setTimeout(() => setShowTypeMenu(false), 120);
                    }}
                    onChange={(e) => {
                      updateSelectedNode('type', e.target.value);
                      setTypeQuery(e.target.value);
                      setShowTypeMenu(true);
                    }}
                    placeholder="Search or create custom type"
                  />
                  {showTypeMenu && (
                    <div className="type-combobox-menu">
                      {allDeliverableTypeOptions.map((option) => (
                        <button
                          key={option}
                          type="button"
                          className="type-combobox-item"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => {
                            updateSelectedNode('type', option);
                            setTypeQuery(option);
                            setShowTypeMenu(false);
                          }}
                        >
                          {option}
                        </button>
                      ))}
                      {!!normalizeText(typeQuery) && !allDeliverableTypeOptions.some((option) => normalizeText(option) === normalizeText(typeQuery)) && (
                        <button
                          type="button"
                          className="type-combobox-item create"
                          onMouseDown={(e) => e.preventDefault()}
                          onClick={() => {
                            updateSelectedNode('type', typeQuery.trim());
                            setShowTypeMenu(false);
                          }}
                        >
                          Use custom: "{typeQuery.trim()}"
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </label>
              <label>Title<input value={selectedNode.title ?? ''} onChange={(e) => updateSelectedNode('title', e.target.value)} /></label>
              <label className="field-full">Description<textarea value={selectedNode.description ?? ''} onChange={(e) => updateSelectedNode('description', e.target.value)} /></label>
              <label>Weight<input type="number" value={selectedNode.weight ?? 0} onChange={(e) => updateSelectedNode('weight', Number(e.target.value))} /></label>
              {!DELIVERABLE_BASE_TYPES.includes(selectedNode.type) && (
                <label className="checkbox-row">
                  <span>File upload deliverable</span>
                  <input
                    type="checkbox"
                    checked={!!selectedNode.fileUploadDeliverable}
                    onChange={(e) => updateSelectedNode('fileUploadDeliverable', e.target.checked)}
                  />
                </label>
              )}
              <label className="checkbox-row">
                <span>Auto add to all sprints</span>
                <input
                  type="checkbox"
                  checked={!!selectedNode.autoAddToAllSprints}
                  onChange={(e) => updateSelectedNode('autoAddToAllSprints', e.target.checked)}
                />
              </label>
              <div className="editor-subsection field-full">
                <div className="editor-subsection-header">
                  <strong>Rubrics</strong>
                  <div className="template-inline-actions">
                    <button
                      className="template-btn add-uniform"
                      onClick={() => addDeliverableRubric(selected.sprintIndex, selected.deliverableIndex)}
                      type="button"
                    >
                      + Add Rubric
                    </button>
                    <button
                      className="template-btn add-uniform"
                      type="button"
                      onClick={() => setClipboard((prev) => ({ ...prev, rubrics: deepClone(selectedNode.rubrics || []) }))}
                    >
                      Copy Rubrics
                    </button>
                    <button
                      className="template-btn add-uniform"
                      type="button"
                      onClick={() => pasteDeliverableRubrics(selected.sprintIndex, selected.deliverableIndex)}
                      disabled={!clipboard.rubrics}
                    >
                      Paste Rubrics
                    </button>
                  </div>
                </div>
                <div className="rubric-inline-list">
                  {(selectedNode.rubrics || []).map((rubric, rubricIndex) => (
                    <div className="rubric-inline-row" key={`d-editor-rubric-${rubricIndex}`}>
                      <label>
                        Title
                        <input
                          value={rubric.title ?? ''}
                          onChange={(e) =>
                            updateDeliverableRubricField(
                              selected.sprintIndex,
                              selected.deliverableIndex,
                              rubricIndex,
                              'title',
                              e.target.value
                            )
                          }
                        />
                      </label>
                      <label>
                        Criteria Type
                        <select
                          value={rubric.criteriaType ?? 'SOFT'}
                          onChange={(e) =>
                            updateDeliverableRubricField(
                              selected.sprintIndex,
                              selected.deliverableIndex,
                              rubricIndex,
                              'criteriaType',
                              e.target.value
                            )
                          }
                        >
                          {GRADING_TYPE_OPTIONS.map((option) => (
                            <option key={option} value={option}>{option}</option>
                          ))}
                        </select>
                      </label>
                      <button
                        type="button"
                        className="delete-node-btn"
                        onClick={() =>
                          removeDeliverableRubric(
                            selected.sprintIndex,
                            selected.deliverableIndex,
                            rubricIndex
                          )
                        }
                        title="Delete rubric"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {selected.type === 'sprintEvaluation' && selectedNode && (
            <div className="template-form">
              <div className="field-full editor-toolbar">
                <button
                  className="template-btn"
                  type="button"
                  onClick={() => setClipboard((prev) => ({ ...prev, evaluation: deepClone(selectedNode) }))}
                >
                  Copy Evaluation
                </button>
                <button
                  className="template-btn"
                  type="button"
                  onClick={() => pasteEvaluation(selected.sprintIndex)}
                  disabled={!clipboard.evaluation}
                >
                  Paste Evaluation
                </button>
              </div>
              <label>Title<input value={selectedNode.title ?? ''} onChange={(e) => updateSelectedNode('title', e.target.value)} /></label>
              <label>Weight<input type="number" value={selectedNode.weight ?? 0} onChange={(e) => updateSelectedNode('weight', Number(e.target.value))} /></label>
              <label className="checkbox-row">
                <span>Auto add to all sprints</span>
                <input
                  type="checkbox"
                  checked={!!selectedNode.autoAddToAllSprints}
                  onChange={(e) => updateSelectedNode('autoAddToAllSprints', e.target.checked)}
                />
              </label>
              <div className="editor-subsection field-full">
                <div className="editor-subsection-header">
                  <strong>Rubrics</strong>
                  <div className="template-inline-actions">
                    <button
                      className="template-btn add-uniform"
                      onClick={() => addSprintEvalRubric(selected.sprintIndex, selected.evaluationIndex)}
                      type="button"
                    >
                      + Add Rubric
                    </button>
                    <button
                      className="template-btn add-uniform"
                      type="button"
                      onClick={() => setClipboard((prev) => ({ ...prev, rubrics: deepClone(selectedNode.rubrics || []) }))}
                    >
                      Copy Rubrics
                    </button>
                    <button
                      className="template-btn add-uniform"
                      type="button"
                      onClick={() => pasteEvaluationRubrics(selected.sprintIndex, selected.evaluationIndex)}
                      disabled={!clipboard.rubrics}
                    >
                      Paste Rubrics
                    </button>
                  </div>
                </div>
                <div className="rubric-inline-list">
                  {(selectedNode.rubrics || []).map((rubric, rubricIndex) => (
                    <div className="rubric-inline-row" key={`e-editor-rubric-${rubricIndex}`}>
                      <label>
                        Title
                        <input
                          value={rubric.title ?? ''}
                          onChange={(e) =>
                            updateSprintEvaluationRubricField(
                              selected.sprintIndex,
                              selected.evaluationIndex,
                              rubricIndex,
                              'title',
                              e.target.value
                            )
                          }
                        />
                      </label>
                      <label>
                        Criteria Type
                        <select
                          value={rubric.criteriaType ?? 'SOFT'}
                          onChange={(e) =>
                            updateSprintEvaluationRubricField(
                              selected.sprintIndex,
                              selected.evaluationIndex,
                              rubricIndex,
                              'criteriaType',
                              e.target.value
                            )
                          }
                        >
                          {GRADING_TYPE_OPTIONS.map((option) => (
                            <option key={option} value={option}>{option}</option>
                          ))}
                        </select>
                      </label>
                      <button
                        type="button"
                        className="delete-node-btn"
                        onClick={() =>
                          removeSprintEvaluationRubric(
                            selected.sprintIndex,
                            selected.evaluationIndex,
                            rubricIndex
                          )
                        }
                        title="Delete rubric"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

export default TemplateBuilder;
