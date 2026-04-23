import React, { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import 'react-datepicker/dist/react-datepicker.css';

/**
 * FilterPanel Bileşeni - Analitik dashboard için filtreleme kontrolleri
 * @param {Object} props
 * @param {Array} props.students - Öğrenci listesi
 * @param {Array} props.groups - Grup listesi
 * @param {Function} props.onFilterChange - Filtre değiştiği zaman callback
 * @param {boolean} props.loading - Yükleme durumu
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
        <h3>Filtreler</h3>
      </div>

      <div className="filter-content">
        {/* Öğrenci Seçimi */}
        <div className="filter-group">
          <label htmlFor="student-select">Öğrenci Seçin:</label>
          <select
            id="student-select"
            value={selectedStudent}
            onChange={(e) => setSelectedStudent(e.target.value)}
            disabled={loading}
          >
            <option value="">-- Tüm Öğrenciler --</option>
            {students &&
              students.map((student) => (
                <option key={student.id} value={student.id}>
                  {student.name} ({student.studentId})
                </option>
              ))}
          </select>
        </div>

        {/* Grup Seçimi */}
        <div className="filter-group">
          <label htmlFor="group-select">Grup Seçin:</label>
          <select
            id="group-select"
            value={selectedGroup}
            onChange={(e) => setSelectedGroup(e.target.value)}
            disabled={loading}
          >
            <option value="">-- Tüm Gruplar --</option>
            {groups &&
              groups.map((group) => (
                <option key={group.id} value={group.id}>
                  {group.name}
                </option>
              ))}
          </select>
        </div>

        {/* Tarih Aralığı */}
        <div className="filter-group date-range">
          <label>Tarih Aralığı:</label>
          <div className="date-inputs">
            <div className="date-input-wrapper">
              <label htmlFor="start-date" className="small-label">
                Başlangıç:
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
                placeholderText="Başlangıç tarihi"
                disabled={loading}
                isClearable
              />
            </div>

            <div className="date-input-wrapper">
              <label htmlFor="end-date" className="small-label">
                Bitiş:
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
                placeholderText="Bitiş tarihi"
                disabled={loading}
                isClearable
              />
            </div>
          </div>
        </div>

        {/* Temizle Butonu */}
        <div className="filter-actions">
          <button
            onClick={handleReset}
            disabled={loading}
            className="btn-secondary"
          >
            Filtreleri Temizle
          </button>
        </div>
      </div>
    </div>
  );
};

export default FilterPanel;
