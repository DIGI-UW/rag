-- Sample initialization script for OpenMRS database
USE openmrs;

-- Create patients table (simplified for example)
CREATE TABLE IF NOT EXISTS patients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT,
    gender VARCHAR(10),
    registration_date DATE
);

-- Insert sample patient data
INSERT INTO patients (name, age, gender, registration_date)
VALUES 
    ('John Doe', 35, 'Male', '2023-01-15'),
    ('Jane Smith', 28, 'Female', '2023-02-20'),
    ('Robert Johnson', 45, 'Male', '2023-03-10'),
    ('Emily Davis', 32, 'Female', '2023-04-05'),
    ('Michael Brown', 50, 'Male', '2023-05-12');

-- Create visits table
CREATE TABLE IF NOT EXISTS visits (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT,
    visit_date DATE,
    reason VARCHAR(200),
    doctor VARCHAR(100),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- Insert sample visit data
INSERT INTO visits (patient_id, visit_date, reason, doctor)
VALUES 
    (1, '2023-02-01', 'Regular checkup', 'Dr. Smith'),
    (2, '2023-03-15', 'Flu symptoms', 'Dr. Johnson'),
    (3, '2023-04-20', 'Back pain', 'Dr. Williams'),
    (1, '2023-05-10', 'Follow-up', 'Dr. Smith'),
    (4, '2023-06-05', 'Headache', 'Dr. Brown');

-- Create prescriptions table
CREATE TABLE IF NOT EXISTS prescriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    visit_id INT,
    medication VARCHAR(100),
    dosage VARCHAR(50),
    instructions TEXT,
    FOREIGN KEY (visit_id) REFERENCES visits(id)
);

-- Insert sample prescription data
INSERT INTO prescriptions (visit_id, medication, dosage, instructions)
VALUES 
    (1, 'Vitamin D', '1000 IU', 'Take once daily with food'),
    (2, 'Amoxicillin', '500mg', 'Take three times daily for 7 days'),
    (3, 'Ibuprofen', '400mg', 'Take as needed for pain, not to exceed 3 tablets per day'),
    (4, 'Lisinopril', '10mg', 'Take once daily in the morning'),
    (5, 'Sumatriptan', '50mg', 'Take at onset of migraine, may repeat after 2 hours if needed');

-- Create lab_results table
CREATE TABLE IF NOT EXISTS lab_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT,
    test_date DATE,
    test_name VARCHAR(100),
    result TEXT,
    reference_range VARCHAR(100),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- Insert sample lab results
INSERT INTO lab_results (patient_id, test_date, test_name, result, reference_range)
VALUES 
    (1, '2023-02-01', 'Blood Glucose', '95 mg/dL', '70-99 mg/dL'),
    (1, '2023-02-01', 'Cholesterol', '210 mg/dL', '<200 mg/dL'),
    (2, '2023-03-15', 'Complete Blood Count', 'WBC: 7.5 K/uL', '4.5-11.0 K/uL'),
    (3, '2023-04-20', 'Vitamin D', '25 ng/mL', '30-100 ng/mL'),
    (4, '2023-06-05', 'Thyroid Panel', 'TSH: 2.5 mIU/L', '0.4-4.0 mIU/L');