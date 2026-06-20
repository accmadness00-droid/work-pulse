import 'package:flutter_test/flutter_test.dart';
import 'package:work_pulse_mobile/features/auth/domain/app_user.dart';

void main() {
  test('parses authenticated employee response', () {
    final user = AppUser.fromJson({
      'id': 'user-id',
      'email': 'employee@workpulse.uz',
      'role': 'EMPLOYEE',
      'companyId': 'company-id',
      'branchId': 'branch-id',
      'employeeId': 'employee-id',
      'permissions': ['CAMERA_ATTENDANCE'],
    });

    expect(user.isEmployee, isTrue);
    expect(user.employeeId, 'employee-id');
    expect(user.permissions, contains('CAMERA_ATTENDANCE'));
  });
}
