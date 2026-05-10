insert into api_token (id, activated, expires_at, hashed_token, salt)
values ('3c727bd5-9822-482e-996b-b8ead0ce687d', true, '2023-10-01 10:00:00', 'tokenHash', 'randomSalt');

insert into contact (id, administration_mail, administration_phone_number, administration_phone_number_two,
                     contact_country, contact_nodes, contact_person, contact_plz, contact_street, emergency_mail,
                     emergency_number, emergency_number_two, location, organization, service_mail, service_number,
                     service_number_two)
values ('f68c2af4-86b1-46df-a4f0-b0450d4b6a8c', 'admin@mail.org', '012345647890', '012345647892', 'Austria',
        'notes to this contact',
        'Patrick Peham', '4339', 'Grünhausen 22', 'alarm@mail.org', '122', '112', 'Amstetten',
        'PegelHub', 'service@mail.org', '0987654321', '0987654320');


insert into connector (id, connector_number, data_definition, nodes, software_version, type_description,
                       works_from_data_version,
                       manufacturer_id, operating_company_id, software_manufacturer_id, technically_responsible_id,api_token)
values ('74a48b34-8a3e-4f5b-aeca-ba006a0e6692', 'number', 'The definition for the sent data',
        'notes to this connector',
        1.5, 'A sample description for a connector', 1.79, 'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c',
        'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c', 'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c',
        'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c','3c727bd5-9822-482e-996b-b8ead0ce687d'),
       ('59a48b34-8a3e-4f5b-aeca-ba006a0e6692', 'number', 'The definition for the sent data',
        'notes to this connector',
        2.5, 'A sample description for a connector', 2.79, 'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c',
        'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c', 'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c',
        'f68c2af4-86b1-46df-a4f0-b0450d4b6a8c','3c727bd5-9822-482e-996b-b8ead0ce687d');

insert into station_manufacturer(id, station_manufacturer_firmware_version, station_manufacturer_name,
                                 station_manufacturer_typ, station_remark)
values ('42685117-0870-41f0-97d3-a58840a05bf9', 'manufacturer version', 'manufacturer name', 'manufacturer type',
        'remarks to this station');

insert into taker_service_manufacturer (id, request_remark, station_manufacturer_firmware_version,
                                        taker_manufacturer_name, taker_system_name)
values ('a0be1463-fdf5-4267-84ce-9ddc53bfeb80', 'remarks to this manufacturer', '1.7.9',
        'name of the manufacturer', 'name of the system');

insert into supplier (id, accuracy, data_critically, hsq, hsq100, hsw, hsw100, hsw_reference,
                      main_usage, mq, mw, mw_reference, refresh_rate,
                      rnq, rnw, rnw_reference, station_base_reference_level, station_id,
                      station_name, station_number,
                      station_reference_place, station_water_kilometer, station_water_latitude, station_water_latitudem,
                      station_water, station_water_longitude, station_water_longtitudem,
                      station_water_type, station_waterside,
                      connector_id, station_manufacturer_id)
values ('f68c2af4-86b1-46df-a4f0-b0450d4b6a8c', 2.5, 'always', 1.5, 1.5, 1, 1, 10,
        'business cases', 1.5, 1, 10, 36000000, 1.5, 1, 10,
        1.5, 1, 'incredible station', '12345', 'inner mind', 1.5,
        1.5, 1.5, 'Ottensheim', 1.5, 1.5, 'S', 'Donau',
        '74a48b34-8a3e-4f5b-aeca-ba006a0e6692',
        '42685117-0870-41f0-97d3-a58840a05bf9');

insert into taker (id, refresh_rate, station_id, station_number, connector_id, taker_service_manufacturer_id)
values ('1c0fd27c-5e46-438c-92c1-cdb1ff6ccb50', 36000000, 1, '10.10.1',
        '74a48b34-8a3e-4f5b-aeca-ba006a0e6692', 'a0be1463-fdf5-4267-84ce-9ddc53bfeb80');