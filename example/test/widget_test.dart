import 'package:flutter_test/flutter_test.dart';

import 'package:connectycube_flutter_call_kit_example/main.dart';

void main() {
  testWidgets('example app renders main controls', (WidgetTester tester) async {
    await tester.pumpWidget(const CallKitExampleApp());
    await tester.pump();

    expect(find.text('CallKit example'), findsOneWidget);
    expect(find.text('Incoming (30s)'), findsOneWidget);
    expect(find.text('Show missed'), findsOneWidget);
  });
}
