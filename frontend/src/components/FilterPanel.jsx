import React, { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import 'react-datepicker/dist/react-datepicker.css';

/**
 * FilterPanel Component - Filtering controls for analytics dashboard
 * @param {Object} props
 * @param {Array} props.students - List of students
 * @param {Array} props.groups - List of groups
 * @param {Function} props.onFilterChange - Callback when filters change
 * @param {boolean} props.loading - Loading state
 */
const FilterPanel = ({ students, groups, onFilterChange, loading }) => {
  const [selectedStudent, setSelectedStudent] = useState('');
  const [selectedGroup, setSelectedGroup] = useState('');
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);

  useEffect(() => {
    if (onFilterChange) {
      onFilterChange({
        studentId: selectedStudent,
        groupId: selectedGroup,
        startDate: startDate ? format(startDate, 'yyyy-MM-dd') : null,
        endDate: endDate ? format(endDate, 'yyyy-MM-dd') : null,
      });
    }
  }, [selectedStudent, selectedGroup, startDate, endDate, onFilterChange]);

  const handleReset = () => {
    setSelectedStudent('');
    setSelectedGroup('');
    setStartDate(null);
    setEndDate(null);
  };

  return (
    <div className="filter-panel">
      <div className="filter-header">
        <h3>Filters</h3>
      </div>

      <div className="filter-content">
        {/* Student Selection */}
        <div className="filter-group">
          <label htmlFor="student-select">Select Student:</label>
          <select
            id="student-select"
            value={selectedStudent}
            onChange={(e) => setSelectedStudent(e.target.value)}
            disabled={loading}
          >
            <option value="">-- All Students --</option>
            {students &&
              students.map((student) => (
                <option key={student.id} value={student.id}>
                  {student.name} ({student.studentId})
                </option>
              ))}
          </select>
        </div>

        {/* Group Selection */}
        <div className="filter-group">
          <label htmlFor="group-select">Select Group:</label>
          <select
            id="group-select"
            value={selectedGroup}
            onChange={(e) => setSelectedGroup(e.target.value)}
            disabled={loading}
          >
            <option value="">-- All Groups --</option>
            {groups &&
              groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.name}
                </option>
              ))}
          </select>
        </div>

        {/* Date Range */}
        <div className="filter-group date-range">
          <label>Date Range:</label>
          <div className="date-inputs">
            <div className="date-input-wrapper">
              <label htmlFor="start-date" className="small-label">
                Start:
              </label>
              <DatePicker
                id="start-date"
                selected={startDate}
                onChange={(date) => setStartDate(date)}
                selectsStart
                startDate={startDate}
                endDate={endDate}
                dateFormat="dd/MM/yyyy"
                locale={tr}
                placeholderText="Start date"
                disabled={loading}
                isClearable
              />
            </div>

            <div className="date-input-wrapper">
              <label htmlFor="end-date" className="small-label">
                End:
              </label>
              <DatePicker
                id="end-date"
                selected={endDate}
                onChange={(date) => setEndDate(date)}
                selectsEnd
                startDate={startDate}
                endDate={endDate}
                minDate={startDate}
                dateFormat="dd/MM/yyyy"
                locale={tr}
                placeholderText="End date"
                disabled={loading}
                isClearable
              />
            </div>
          </div>
        </div>

        {/* Clear Button */}
        <div className="filter-actions">
          <button
            onClick={handleReset}
            disabled={loading}
            className="btn-secondary"
          >
            Clear Filters
          </button>
        </div>
      </div>
    </div>
  );
};

export default FilterPanel;
