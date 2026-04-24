CREATE TABLE provinces (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    full_name_en VARCHAR(255),
    code_name VARCHAR(255)
);

CREATE TABLE districts (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    full_name_en VARCHAR(255),
    code_name VARCHAR(255),
    province_code VARCHAR(20),
    CONSTRAINT fk_district_province FOREIGN KEY (province_code) REFERENCES provinces(code)
);

CREATE TABLE wards (
    code VARCHAR(20) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    name_en VARCHAR(255),
    full_name VARCHAR(255) NOT NULL,
    full_name_en VARCHAR(255),
    code_name VARCHAR(255),
    district_code VARCHAR(20),
    CONSTRAINT fk_ward_district FOREIGN KEY (district_code) REFERENCES districts(code)
);
